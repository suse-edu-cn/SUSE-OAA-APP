package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.shared.data.repository.AcademicStatusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 学业情况查询 ViewModel
 * 显示学生的课程修读状态、学分完成情况等
 * 使用教务系统同款绩点计算方式（直接使用JD字段）
 */
class AcademicStatusViewModel(
    private val academicStatusRepository: AcademicStatusRepository,
    private val tokenManager: TokenManager
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
            val studentId = tokenManager.currentStudentId.first()
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
            val studentId = tokenManager.currentStudentId.first() ?: return@launch

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
