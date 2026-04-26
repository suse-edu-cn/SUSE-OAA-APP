package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldCheckinTask
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldDiscoveryManager
import com.suseoaa.projectoaa.shared.data.repository.NearFieldCheckinRepository
import com.suseoaa.projectoaa.shared.data.repository.PersonRepository
import com.suseoaa.projectoaa.shared.database.NearFieldTask
import com.suseoaa.projectoaa.shared.database.NearFieldParticipant
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 活动签到的业务逻辑层(ViewModel)
 */
class ActivityCheckinViewModel(
    private val checkinRepository: NearFieldCheckinRepository,
    private val personRepository: PersonRepository
) : ViewModel() {
    private val discoveryManager = NearFieldDiscoveryManager()
    private val _uiState = MutableStateFlow(ActivityCheckinUiState())
    val uiState: StateFlow<ActivityCheckinUiState> = _uiState.asStateFlow()

    init {
        // 监听发现的任务
        viewModelScope.launch {
            discoveryManager.discoveredTasks.collect { tasks ->
                _uiState.update { it.copy(discoveredTasks = tasks) }
            }
        }

        // 监听扫描和广播状态
        discoveryManager.isScanning.onEach {
            val scanning = it; _uiState.update { it.copy(isScanning = scanning) }
        }.launchIn(viewModelScope)
        discoveryManager.isBroadcasting.onEach {
            val broadcasting = it; _uiState.update {
            it.copy(
                isBroadcasting = broadcasting
            )
        }
        }.launchIn(viewModelScope)

        // 监听本地任务历史
        checkinRepository.getTaskHistory()
            .onEach { history -> _uiState.update { it.copy(taskHistory = history) } }
            .launchIn(viewModelScope)
    }

    /**
     * 切换扫描状态
     */
    fun toggleScanning() {
        if (_uiState.value.isScanning) discoveryManager.stopScanning() else discoveryManager.startScanning()
    }

    /**
     * 开启扫描
     */
    fun startScanning() {
        if (!_uiState.value.isScanning) discoveryManager.startScanning()
    }

    /**
     * 停止扫描
     */
    fun stopScanning() {
        if (_uiState.value.isScanning) discoveryManager.stopScanning()
    }

    /**
     * 开始作为主机发布任务
     */
    fun startHosting(
        activityName: String,
        hostName: String,
        startTime: Long,
        endTime: Long,
        existingTaskIdentifier: String? = null,
        securityNonce: String? = null
    ) {
        val now = OaaClock.now().toEpochMilliseconds()
        val task = NearFieldCheckinTask(
            taskIdentifier = existingTaskIdentifier ?: "TASK_${now}",
            activityName = activityName,
            hostDeviceName = hostName,
            publishTimestamp = now,
            startTime = startTime,
            endTime = endTime,
            securityNonce = securityNonce ?: (100000..999999).random().toString(),
            hostPort = 8888 // 默认端口
        )

        viewModelScope.launch {
            checkinRepository.saveTask(task, isMyHosted = true)
            checkinRepository.startCheckinServer(8888, task.taskIdentifier)
            discoveryManager.startBroadcasting(task)

            // 记录当前正在广播的任务标识
            _uiState.update { it.copy(hostingTaskIdentifier = task.taskIdentifier) }

            // 自动加载当前任务的参与者
            loadParticipants(task.taskIdentifier)
        }
    }

    /**
     * 停止发布
     */
    fun stopHosting() {
        discoveryManager.stopBroadcasting()
        checkinRepository.stopCheckinServer()
        _uiState.update { it.copy(hostingTaskIdentifier = null) }
    }

    /**
     * 执行签到
     */
    fun performCheckin(task: NearFieldCheckinTask, customName: String? = null) {
        viewModelScope.launch {
            val now = OaaClock.now().toEpochMilliseconds()
            if (!task.isValid(now)) {
                _uiState.update { it.copy(errorMessage = "该签到任务不在有效时间内") }
                return@launch
            }

            val person = personRepository.getPersonInfo().getOrNull()
            val studentName = customName ?: person?.name ?: "未知同学"
            val studentId = person?.studentId ?: person?.username ?: "000000"

            val result = checkinRepository.sendCheckinRequest(task, studentName, studentId)
            if (result.isSuccess) {
                _uiState.update { it.copy(checkinSuccess = true, lastCheckinTask = task) }
            } else {
                _uiState.update { it.copy(errorMessage = "签到失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    /**
     * 手动为任务添加参与者（补签/后台录入）
     */
    fun manualAddParticipant(
        taskIdentifier: String,
        name: String,
        studentId: String,
        status: String
    ) {
        viewModelScope.launch {
            checkinRepository.manualAddParticipant(taskIdentifier, name, studentId, status)
        }
    }

    /**
     * 删除整个签到任务记录
     */
    fun deleteTask(taskIdentifier: String) {
        viewModelScope.launch {
            checkinRepository.deleteTask(taskIdentifier)
            if (_uiState.value.hostingTaskIdentifier == taskIdentifier) {
                stopHosting()
            }
        }
    }

    /**
     * 删除单个参与者记录
     */
    fun deleteParticipant(id: Long, taskIdentifier: String) {
        viewModelScope.launch {
            checkinRepository.deleteParticipant(id)
            // 重新加载该任务的参与者列表
            loadParticipants(taskIdentifier)
        }
    }

    /**
     * 获取当前用户信息（用于签到确认）
     */
    suspend fun getCurrentPerson() = personRepository.getPersonInfo().getOrNull()

    /**
     * 加载特定任务的参与者列表
     */
    fun loadParticipants(taskIdentifier: String) {
        viewModelScope.launch {
            checkinRepository.getParticipants(taskIdentifier).collect { participants ->
                _uiState.update { it.copy(currentParticipants = participants) }
            }
        }
    }

    /**
     * 从主机同步最新的参与者名单
     */
    fun syncParticipants(taskIdentifier: String) {
        // 首先在发现的任务中寻找匹配的主机信息
        val discoveredTask =
            _uiState.value.discoveredTasks.find { it.taskIdentifier == taskIdentifier }
        if (discoveredTask == null) {
            _uiState.update { it.copy(errorMessage = "未在附近发现该任务的主机，无法同步") }
            return
        }

        viewModelScope.launch {
            val result = checkinRepository.syncParticipants(discoveredTask)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "同步失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun resetCheckinStatus() {
        _uiState.update { it.copy(checkinSuccess = false) }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.release()
        checkinRepository.stopCheckinServer()
    }
}

/**
 * 界面状态模型
 */
data class ActivityCheckinUiState(
    /**
     * 发现的签到任务列表
     */
    val discoveredTasks: List<NearFieldCheckinTask> = emptyList(),

    /**
     * 本地签到任务历史
     */
    val taskHistory: List<NearFieldTask> = emptyList(),

    /**
     * 当前选定任务的参与者列表
     */
    val currentParticipants: List<NearFieldParticipant> = emptyList(),

    /**
     * 是否正在扫描
     */
    val isScanning: Boolean = false,

    /**
     * 是否正在广播（作为主机）
     */
    val isBroadcasting: Boolean = false,

    /**
     * 当前正在广播的任务标识
     */
    val hostingTaskIdentifier: String? = null,

    /**
     * 签到是否成功
     */
    val checkinSuccess: Boolean = false,

    /**
     * 最后一次成功签到的任务
     */
    val lastCheckinTask: NearFieldCheckinTask? = null,

    /**
     * 错误提示信息
     */
    val errorMessage: String? = null
)
