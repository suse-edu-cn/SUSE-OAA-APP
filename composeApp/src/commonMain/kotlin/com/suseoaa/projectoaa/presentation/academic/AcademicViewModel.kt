package com.suseoaa.projectoaa.presentation.academic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.repository.ExamCacheEntity
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.MessageCacheEntity
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.util.parseExamTimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 考试响应
 */
@Serializable
data class ExamResponse(
    @SerialName("items")
    val items: List<ExamItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

/**
 * 考试信息条目 - 匹配教务系统返回的字段
 */
@Serializable
data class ExamItem(
    @SerialName("kcmc")
    val kcmc: String? = "",   // 课程名称: "网络安全技术"
    @SerialName("kssj")
    val kssj: String? = "",   // 考试时间: "2026-01-08(09:30-11:30)"
    @SerialName("cdmc")
    val cdmc: String? = "",   // 教室名称: "LA5-322"
    @SerialName("cdxqmc")
    val cdxqmc: String? = "", // 校区: "临港校区"
    @SerialName("zw")
    val zw: String? = "",     // 座位号
    @SerialName("xh")
    val xh: String? = "",     // 学号
    @SerialName("xm")
    val xm: String? = ""      // 姓名
)

/**
 * 考试UI状态
 */
data class ExamUiState(
    val courseName: String,
    val time: String,
    val location: String
)

data class AcademicUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<MessageCacheEntity> = emptyList(),
    val exams: List<ExamUiState> = emptyList(),
    val errorMessage: String? = null
)

class AcademicViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolInfoRepository: SchoolInfoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AcademicUiState())
    val uiState: StateFlow<AcademicUiState> = _uiState.asStateFlow()

    // 当前账户流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(localCourseRepository.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 考试列表流 (含精确排序逻辑)
    @OptIn(ExperimentalCoroutinesApi::class)
    val examList: StateFlow<List<ExamUiState>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            schoolInfoRepository.observeExams(studentId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        .let { flow ->
            MutableStateFlow(emptyList<ExamUiState>()).also { resultFlow ->
                viewModelScope.launch {
                    flow.collect { entities ->
                        val timeZone = TimeZone.currentSystemDefault()
                        val now = Clock.System.now().toLocalDateTime(timeZone)

                        val sorted = entities.map { entity ->
                            ExamUiState(
                                courseName = entity.courseName,
                                time = entity.time,
                                location = entity.location
                            )
                        }.sortedWith { a, b ->
                            // 使用工具类解析完整时间
                            val timesA = parseExamTimeRange(a.time)
                            val timesB = parseExamTimeRange(b.time)

                            // 异常处理：解析失败的项放到最后
                            if (timesA == null && timesB == null) return@sortedWith 0
                            if (timesA == null) return@sortedWith 1
                            if (timesB == null) return@sortedWith -1

                            val (startA, endA) = timesA
                            val (startB, endB) = timesB

                            // 判断是否已结束
                            val isEndedA = now > endA
                            val isEndedB = now > endB

                            if (isEndedA != isEndedB) {
                                if (isEndedA) 1 else -1
                            } else {
                                startA.compareTo(startB)
                            }
                        }
                        resultFlow.value = sorted
                    }
                }
            }
        }

    // 消息列表流
    @OptIn(ExperimentalCoroutinesApi::class)
    val messageList: StateFlow<List<MessageCacheEntity>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            schoolInfoRepository.observeMessages(studentId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // 观察考试数据变化
        viewModelScope.launch {
            examList.collect { exams ->
                _uiState.update { it.copy(exams = exams) }
            }
        }

        // 观察消息数据变化
        viewModelScope.launch {
            messageList.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        
        // 监听账号切换，自动刷新数据
        viewModelScope.launch {
            var previousStudentId: String? = null
            tokenManager.currentStudentId.collect { studentId ->
                if (studentId != null && previousStudentId != null && studentId != previousStudentId) {
                    // 账号切换了，自动刷新
                    refresh()
                }
                previousStudentId = studentId
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // 数据会通过 Flow 自动更新
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                // 刷新考试信息
                schoolInfoRepository.refreshAcademicExamInfo(account)
                // 刷新调课通知
                schoolInfoRepository.refreshAcademicMessageInfo(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "刷新失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshExams() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                schoolInfoRepository.refreshAcademicExamInfo(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "刷新考试信息失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshMessages() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                schoolInfoRepository.refreshAcademicMessageInfo(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "刷新消息失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
