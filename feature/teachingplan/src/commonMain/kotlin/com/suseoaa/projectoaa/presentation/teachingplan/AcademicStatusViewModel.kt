package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.shared.domain.repository.AcademicStatusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore

/**
 * 以下 UI 状态原本定义在 shared 的 domain/model 里，属于 UI 层反向下沉到领域层。
 * 它们只被本模块的 ViewModel 与 Screen 使用，现归位到 presentation 层。
 */
data class AcademicStatusUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categories: List<AcademicStatusCategory> = emptyList(),
    val expandedCategories: Set<String> = emptySet(),
    val selectedFilter: AcademicStatusFilter = AcademicStatusFilter.ALL,
    val errorMessage: String? = null,
    val totalCredits: Double = 0.0,
    val earnedCredits: Double = 0.0,
    val studyingCredits: Double = 0.0,
    val averageGradePoint: Double = 0.0,
    // 教务系统原始的总体学分要求
    val planOverview: AcademicPlanOverview = AcademicPlanOverview(),
    // 其它课程学分要求的课程（qtkcxfyq节点）
    val otherCourses: List<AcademicStatusCourseItem> = emptyList(),
    val otherCoursesPassedCount: Int = 0,
    val otherCoursesTotalCount: Int = 0,
    // 计划内课程统计
    val planTotalCourses: Int = 0,
    val planPassedCount: Int = 0,
    val planFailedCount: Int = 0,
    val planStudyingCount: Int = 0,
    val planNotStudiedCount: Int = 0,
    // 计划外课程统计
    val nonPlanCourses: List<AcademicStatusCourseItem> = emptyList(),
    val nonPlanPassedCount: Int = 0,
    val nonPlanFailedCount: Int = 0
)

enum class AcademicStatusFilter(val displayName: String) {
    ALL("全部"),
    PASSED("已通过"),
    FAILED("不及格"),
    STUDYING("在修"),
    NOT_STUDIED("未修")
}

/**
 * 判断某个修读状态码是否命中当前筛选项。
 *
 * 原本定义在 shared 的 StudyStatusUtils 里，导致领域层反过来依赖 UI 的筛选枚举；
 * 状态码常量仍由 shared 的 StudyStatusUtils 提供，这里只承担 UI 筛选语义。
 */
fun StudyStatusUtils.matchesFilter(statusCode: String, filter: AcademicStatusFilter): Boolean =
    when (filter) {
        AcademicStatusFilter.ALL -> true
        AcademicStatusFilter.PASSED -> statusCode == StudyStatusUtils.PASSED
        AcademicStatusFilter.FAILED -> statusCode == StudyStatusUtils.FAILED
        AcademicStatusFilter.STUDYING -> statusCode == StudyStatusUtils.STUDYING
        AcademicStatusFilter.NOT_STUDIED -> statusCode == StudyStatusUtils.NOT_STUDIED
    }


/**
 * 学业情况查询 ViewModel
 * 显示学生的课程修读状态、学分完成情况等
 * 使用教务系统同款绩点计算方式（直接使用JD字段）
 */
class AcademicStatusViewModel(
    private val academicStatusRepository: AcademicStatusRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicStatusUiState())
    val uiState: StateFlow<AcademicStatusUiState> = _uiState.asStateFlow()

    // 缓存HTML内容，用于"其它课程"请求
    private var cachedHtmlContent: String = ""

    init {
        loadAcademicStatus()
    }

    /**
     * 加载学业情况
     */
    fun loadAcademicStatus() {
        viewModelScope.launch {
            val studentId = sessionStore.currentStudentId.first()
            if (studentId == null) {
                _uiState.update { it.copy(errorMessage = "请先登录教务系统") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = academicStatusRepository.getAcademicStatusCategories(studentId)
            result.fold(
                onSuccess = { (planOverview, categories) ->
                    _uiState.update { state ->
                        state.copy(
                            planOverview = planOverview,
                            categories = categories,
                            isLoading = false,
                            // 默认展开所有类别
                            expandedCategories = categories.map { it.categoryId }.toSet()
                        )
                    }
                    // 加载每个类别的课程详情
                    loadAllCategoryCourses(studentId, categories)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "加载学业情况失败: ${error.message}",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 加载所有类别的课程
     */
    private suspend fun loadAllCategoryCourses(
        studentId: String,
        categories: List<AcademicStatusCategory>
    ) {
        for (category in categories) {
            loadCategoryCourses(studentId, category.categoryId)
        }
        // 加载所有类别的非计划课程
        loadAllNonPlanCourses(studentId, categories)
        // 计算总体统计
        calculateTotalStats()
    }

    /**
     * 加载所有类别的非计划课程（计划外课程）
     */
    private suspend fun loadAllNonPlanCourses(
        studentId: String,
        categories: List<AcademicStatusCategory>
    ) {
        val allNonPlanCourses = mutableListOf<AcademicStatusCourseItem>()
        for (category in categories) {
            val result = academicStatusRepository.getNonPlanCourses(category.categoryId, studentId)
            result.onSuccess { courses ->
                allNonPlanCourses.addAll(courses)
            }
        }
        val passedCount = allNonPlanCourses.count { it.studyStatus == StudyStatusUtils.PASSED }
        val failedCount = allNonPlanCourses.count { it.studyStatus == StudyStatusUtils.FAILED }
        _uiState.update {
            it.copy(
                nonPlanCourses = allNonPlanCourses,
                nonPlanPassedCount = passedCount,
                nonPlanFailedCount = failedCount
            )
        }
    }

    /**
     * 加载单个类别的课程
     */
    private suspend fun loadCategoryCourses(studentId: String, categoryId: String) {
        // 标记为正在加载
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map { cat ->
                    if (cat.categoryId == categoryId) {
                        cat.copy(isLoading = true)
                    } else cat
                }
            )
        }

        val result = academicStatusRepository.getCategoryCourses(categoryId, studentId)
        result.fold(
            onSuccess = { courses ->
                val stats = academicStatusRepository.calculateCategoryStats(courses)
                _uiState.update { state ->
                    state.copy(
                        categories = state.categories.map { cat ->
                            if (cat.categoryId == categoryId) {
                                cat.copy(
                                    courses = courses,
                                    isLoading = false,
                                    isLoaded = true,
                                    totalCredits = stats.totalCredits,
                                    earnedCredits = stats.earnedCredits,
                                    passedCount = stats.passedCount,
                                    failedCount = stats.failedCount,
                                    studyingCount = stats.studyingCount,
                                    notStudiedCount = stats.notStudiedCount
                                )
                            } else cat
                        }
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(
                        categories = state.categories.map { cat ->
                            if (cat.categoryId == categoryId) {
                                cat.copy(isLoading = false, isLoaded = true)
                            } else cat
                        }
                    )
                }
            }
        )
    }

    /**
     * 计算总体统计数据
     * 使用教务系统同款绩点：直接使用服务器返回的 JD 字段
     * 加权平均绩点 = Σ(课程绩点 × 课程学分) / Σ(课程学分)
     * 不及格课程（JD=0）也参与计算，拉低平均绩点
     */
    private fun calculateTotalStats() {
        val state = _uiState.value
        var totalCredits = 0.0
        var earnedCredits = 0.0
        var studyingCredits = 0.0
        var planTotalCourses = 0
        var planPassedCount = 0
        var planFailedCount = 0
        var planStudyingCount = 0
        var planNotStudiedCount = 0

        // 收集所有课程用于计算绩点
        val allCourses = mutableListOf<AcademicStatusCourseItem>()

        for (category in state.categories) {
            totalCredits += category.totalCredits
            earnedCredits += category.earnedCredits
            planTotalCourses += category.courses.size
            planPassedCount += category.passedCount
            planFailedCount += category.failedCount
            planStudyingCount += category.studyingCount
            planNotStudiedCount += category.notStudiedCount
            allCourses.addAll(category.courses)

            for (course in category.courses) {
                val credits = course.credits.toDoubleOrNull() ?: 0.0
                if (course.studyStatus == StudyStatusUtils.STUDYING) {
                    studyingCredits += credits
                }
            }
        }

        // 使用教务系统同款绩点计算（包含不及格课程）
        val averageGradePoint = academicStatusRepository.calculateWeightedGpa(allCourses)

        _uiState.update {
            it.copy(
                totalCredits = totalCredits,
                earnedCredits = earnedCredits,
                studyingCredits = studyingCredits,
                averageGradePoint = averageGradePoint,
                planTotalCourses = planTotalCourses,
                planPassedCount = planPassedCount,
                planFailedCount = planFailedCount,
                planStudyingCount = planStudyingCount,
                planNotStudiedCount = planNotStudiedCount
            )
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            val studentId = sessionStore.currentStudentId.first() ?: return@launch

            _uiState.update { it.copy(isRefreshing = true) }

            val result = academicStatusRepository.getAcademicStatusCategories(studentId)
            result.fold(
                onSuccess = { (planOverview, categories) ->
                    _uiState.update { state ->
                        state.copy(
                            planOverview = planOverview,
                            categories = categories,
                            isRefreshing = false
                        )
                    }
                    loadAllCategoryCourses(studentId, categories)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "刷新失败: ${error.message}",
                            isRefreshing = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 切换类别展开状态
     */
    fun toggleCategoryExpanded(categoryId: String) {
        _uiState.update { state ->
            val newSet = if (state.expandedCategories.contains(categoryId)) {
                state.expandedCategories - categoryId
            } else {
                state.expandedCategories + categoryId
            }
            state.copy(expandedCategories = newSet)
        }
    }

    /**
     * 检查类别是否展开
     */
    fun isCategoryExpanded(categoryId: String): Boolean {
        return _uiState.value.expandedCategories.contains(categoryId)
    }

    /**
     * 设置筛选条件
     */
    fun setFilter(filter: AcademicStatusFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /**
     * 获取筛选后的课程列表
     */
    fun getFilteredCourses(courses: List<AcademicStatusCourseItem>): List<AcademicStatusCourseItem> {
        val filter = _uiState.value.selectedFilter
        return if (filter == AcademicStatusFilter.ALL) {
            courses
        } else {
            courses.filter { StudyStatusUtils.matchesFilter(it.studyStatus, filter) }
        }
    }

    /**
     * 全部展开
     */
    fun expandAllCategories() {
        _uiState.update { state ->
            state.copy(expandedCategories = state.categories.map { it.categoryId }.toSet())
        }
    }

    /**
     * 全部折叠
     */
    fun collapseAllCategories() {
        _uiState.update { it.copy(expandedCategories = emptySet()) }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
