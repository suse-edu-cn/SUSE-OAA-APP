package com.suseoaa.projectoaa.presentation.exam

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamCacheEntity
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamApiItem
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamApiResponse
import com.suseoaa.projectoaa.shared.util.getCurrentTerm
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.widget.WidgetRefresher
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.domain.exam.ExamListBuilder
import com.suseoaa.projectoaa.domain.exam.ExamUiItem
import com.suseoaa.projectoaa.domain.exam.SemesterOption
import com.suseoaa.projectoaa.domain.exam.SemesterOptionsBuilder

/**
 * 考试页面 UI 状态
 */
@Immutable
data class ExamUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val exams: List<ExamUiItem> = emptyList(),
    val errorMessage: String? = null,

    // 学期筛选
    val availableSemesters: List<SemesterOption> = emptyList(),
    val selectedYear: String = "",
    val selectedSemester: String = "",

    // 筛选面板展开状态（手机端）
    val isFilterExpanded: Boolean = false,

    // 编辑对话框状态
    val showEditDialog: Boolean = false,
    val editingExam: ExamUiItem? = null,
    val isAddMode: Boolean = false
)

class ExamViewModel(
    private val sessionStore: SessionStore,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolInfoRepository: SchoolInfoRepository,
    private val widgetRefresher: WidgetRefresher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    // 当前账户流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount: StateFlow<CourseAccountEntity?> = sessionStore.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(localCourseRepository.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // 初始化时设置当前学期
        val (currentYear, currentSemester) = getCurrentTerm()
        _uiState.update {
            it.copy(
                selectedYear = currentYear,
                selectedSemester = currentSemester
            )
        }

        // 监听账户变化，动态生成学期选项
        viewModelScope.launch {
            currentAccount.filterNotNull().collect { account ->
                generateSemesterOptions(account.njdmId)
                loadExams()
            }
        }
    }

    /**
     * 选择学期
     */
    fun selectSemester(option: SemesterOption) {
        _uiState.update {
            it.copy(
                selectedYear = option.year,
                selectedSemester = option.semester
                // 不自动折叠，让用户可以继续切换学期
            )
        }
        loadExams()
    }

    /**
     * 切换筛选面板展开状态
     */
    fun toggleFilterExpanded() {
        _uiState.update { it.copy(isFilterExpanded = !it.isFilterExpanded) }
    }

    /**
     * 加载考试信息
     */
    fun loadExams() {
        val account = currentAccount.value ?: return
        val year = _uiState.value.selectedYear
        val semester = _uiState.value.selectedSemester

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 获取 API 考试数据
                val result = schoolInfoRepository.fetchExamsByTerm(account, year, semester)

                // 获取本地自定义考试
                val customExams = schoolInfoRepository.getCustomExamsBySemester(
                    account.studentId, year, semester
                )

                result.fold(
                    onSuccess = { apiItems ->
                        // 处理 API 考试
                        val apiUiItems = processExamItems(apiItems)
                        // 处理自定义考试
                        val customUiItems = processCustomExams(customExams)
                        // 合并并排序
                        val allItems = (apiUiItems + customUiItems).sortedWith { a, b ->
                            val timesA = parseExamTimeRange(a.time)
                            val timesB = parseExamTimeRange(b.time)

                            if (timesA == null && timesB == null) return@sortedWith 0
                            if (timesA == null) return@sortedWith 1
                            if (timesB == null) return@sortedWith -1

                            if (a.isEnded != b.isEnded) {
                                if (a.isEnded) 1 else -1
                            } else {
                                timesA.first.compareTo(timesB.first)
                            }
                        }
                        _uiState.update { it.copy(exams = allItems, isLoading = false) }
                    },
                    onFailure = { e ->
                        // API 失败时仍然显示自定义考试
                        val customUiItems = processCustomExams(customExams)
                        _uiState.update {
                            it.copy(
                                exams = customUiItems,
                                errorMessage = e.message ?: "获取考试信息失败",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "网络错误",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 刷新考试信息
     */
    fun refresh() {
        val account = currentAccount.value ?: return
        val year = _uiState.value.selectedYear
        val semester = _uiState.value.selectedSemester

        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            try {
                val result = schoolInfoRepository.fetchExamsByTerm(account, year, semester)

                result.fold(
                    onSuccess = { items ->
                        val uiItems = processExamItems(items)
                        _uiState.update { it.copy(exams = uiItems) }
                        widgetRefresher.refreshExamWidgets()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(errorMessage = e.message ?: "刷新失败") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "网络错误") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 获取当前选中的学期显示名称
     */
    fun getSelectedSemesterDisplay(): String {
        val year = _uiState.value.selectedYear
        val semester = _uiState.value.selectedSemester

        return _uiState.value.availableSemesters
            .find { it.year == year && it.semester == semester }
            ?.displayName ?: "选择学期"
    }

    // ==================== 考试增删改操作 ====================

    /**
     * 显示添加考试对话框
     */
    fun showAddExamDialog() {
        val year = _uiState.value.selectedYear
        val semester = _uiState.value.selectedSemester
        val semesterName = when (semester) {
            "3" -> "第1学期"
            "12" -> "第2学期"
            "16" -> "第3学期"
            else -> "第?学期"
        }

        _uiState.update {
            it.copy(
                showEditDialog = true,
                isAddMode = true,
                editingExam = ExamUiItem(
                    id = 0,
                    courseName = "",
                    examName = "",
                    time = "",
                    location = "",
                    credit = "",
                    examType = "考试",
                    yearSemester = "$year-${year.toIntOrNull()?.plus(1) ?: ""} $semesterName",
                    isCustom = true
                )
            )
        }
    }

    /**
     * 显示编辑考试对话框
     */
    fun showEditExamDialog(exam: ExamUiItem) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                isAddMode = false,
                editingExam = exam
            )
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                editingExam = null,
                isAddMode = false
            )
        }
    }

    /**
     * 保存考试信息（添加或更新）
     */
    fun saveExam(exam: ExamUiItem) {
        val account = currentAccount.value ?: return
        val year = _uiState.value.selectedYear
        val semester = _uiState.value.selectedSemester

        viewModelScope.launch {
            try {
                val entity = ExamCacheEntity(
                    id = exam.id,
                    studentId = account.studentId,
                    courseName = exam.courseName,
                    time = exam.time,
                    location = exam.location,
                    credit = exam.credit,
                    examType = exam.examType,
                    examName = exam.examName,
                    yearSemester = exam.yearSemester,
                    isCustom = true,
                    xnm = year,
                    xqm = semester
                )

                if (_uiState.value.isAddMode) {
                    schoolInfoRepository.addCustomExam(entity)
                } else {
                    schoolInfoRepository.updateExam(entity)
                }

                hideEditDialog()
                loadExams() // 刷新列表
                widgetRefresher.refreshExamWidgets() // 同步刷新桌面小组件
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "保存失败") }
            }
        }
    }

    /**
     * 删除考试信息
     */
    fun deleteExam(exam: ExamUiItem) {
        viewModelScope.launch {
            try {
                schoolInfoRepository.deleteExam(exam.id)
                hideEditDialog()
                loadExams() // 刷新列表
                widgetRefresher.refreshExamWidgets() // 同步刷新桌面小组件
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "删除失败") }
            }
        }
    }

    /** 当前时间统一在这里取一次，再传给纯函数，方便测试时替换。 */
    private fun nowLocal() = com.suseoaa.projectoaa.shared.util.OaaClock.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    private fun processExamItems(items: List<ExamApiItem>): List<ExamUiItem> =
        ExamListBuilder.fromApiItems(items, nowLocal())

    private fun processCustomExams(exams: List<ExamCacheEntity>): List<ExamUiItem> =
        ExamListBuilder.fromCustomExams(exams, nowLocal())

    private fun generateSemesterOptions(njdmId: String) {
        val now = nowLocal()
        val options = SemesterOptionsBuilder.build(njdmId, now.year, now.monthNumber)
        _uiState.update { it.copy(availableSemesters = options) }
    }

}
