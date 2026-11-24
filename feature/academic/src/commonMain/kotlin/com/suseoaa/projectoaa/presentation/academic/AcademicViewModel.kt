package com.suseoaa.projectoaa.presentation.academic

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.message.MessageCacheEntity
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAccountRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import com.suseoaa.projectoaa.util.SchoolAccountPromptManager
import com.suseoaa.projectoaa.widget.WidgetRefresher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 考试UI状态
 */
@Immutable
data class ExamUiState(
    val courseName: String,
    val time: String,
    val location: String
)

@Immutable
data class AcademicUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<MessageCacheEntity> = emptyList(),
    val exams: List<ExamUiState> = emptyList(),
    val errorMessage: String? = null,
)

class AcademicViewModel(
    private val schoolAccountRepository: SchoolAccountRepository,
    private val schoolInfoRepository: SchoolInfoRepository,
    private val widgetRefresher: WidgetRefresher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicUiState())
    val uiState: StateFlow<AcademicUiState> = _uiState.asStateFlow()

    // 当前默认教务账户流（随数据库新增/切换实时更新）
    private val currentAccount: StateFlow<CourseAccountEntity?> =
        schoolAccountRepository.observeCurrentAccount()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 考试列表流 (含精确排序逻辑)
    @OptIn(ExperimentalCoroutinesApi::class)
    val examList: StateFlow<List<ExamUiState>> = currentAccount
        .filterNotNull()
        .map { it.studentId }
        .distinctUntilChanged()
        .flatMapLatest { studentId ->
            schoolInfoRepository.observeExams(studentId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        .let { flow ->
            MutableStateFlow(emptyList<ExamUiState>()).also { resultFlow ->
                viewModelScope.launch {
                    flow.collect { entities ->
                        val timeZone = TimeZone.currentSystemDefault()
                        val now = OaaClock.now().toLocalDateTime(timeZone)

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
    val messageList: StateFlow<List<MessageCacheEntity>> = currentAccount
        .filterNotNull()
        .map { it.studentId }
        .distinctUntilChanged()
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

        // 监听账号新增或切换，自动刷新数据
        viewModelScope.launch {
            currentAccount
                .filterNotNull()
                .map { it.studentId }
                .distinctUntilChanged()
                .collect {
                    refresh(promptIfMissing = false)
                }
        }
    }

    /**
     * 执行需要教务账号的操作；若当前没有已添加的教务账号，先弹出添加弹窗，成功后自动继续。
     */
    fun requireSchoolAccount(featureName: String, onReady: () -> Unit) {
        viewModelScope.launch {
            val account = schoolAccountRepository.getCurrentAccount()
            if (account != null) {
                onReady()
            } else {
                SchoolAccountPromptManager.requestAddAccount(
                    featureName = featureName,
                    onAccountAdded = onReady
                )
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh(promptIfMissing: Boolean = false) {
        viewModelScope.launch {
            val account = schoolAccountRepository.getCurrentAccount()
            if (account == null) {
                if (promptIfMissing) {
                    SchoolAccountPromptManager.requestAddAccount(
                        featureName = "教务信息",
                        onAccountAdded = { refresh(promptIfMissing = false) }
                    )
                }
                return@launch
            }
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                // 刷新考试信息
                schoolInfoRepository.refreshAcademicExamInfo(account)
                // 刷新调课通知
                schoolInfoRepository.refreshAcademicMessageInfo(account)
                // 同步刷新桌面小组件
                widgetRefresher.refreshExamWidgets()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "刷新失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshExams(promptIfMissing: Boolean = false) {
        viewModelScope.launch {
            val account = schoolAccountRepository.getCurrentAccount()
            if (account == null) {
                if (promptIfMissing) {
                    SchoolAccountPromptManager.requestAddAccount(
                        featureName = "考试信息查询",
                        onAccountAdded = { refreshExams(promptIfMissing = false) }
                    )
                }
                return@launch
            }
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                schoolInfoRepository.refreshAcademicExamInfo(account)
                widgetRefresher.refreshExamWidgets() // 同步刷新桌面小组件
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "刷新考试信息失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshMessages(promptIfMissing: Boolean = false) {
        viewModelScope.launch {
            val account = schoolAccountRepository.getCurrentAccount()
            if (account == null) {
                if (promptIfMissing) {
                    SchoolAccountPromptManager.requestAddAccount(
                        featureName = "最新调课",
                        onAccountAdded = { refreshMessages(promptIfMissing = false) }
                    )
                }
                return@launch
            }
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
