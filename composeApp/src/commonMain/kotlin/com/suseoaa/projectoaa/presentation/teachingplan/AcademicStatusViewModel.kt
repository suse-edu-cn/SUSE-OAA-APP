package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.data.repository.AcademicStatusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 学业情况查询 ViewModel
 * 显示学生的课程修读状态、学分完成情况等
 */
class AcademicStatusViewModel(
    private val academicStatusRepository: AcademicStatusRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicStatusUiState())
    val uiState: StateFlow<AcademicStatusUiState> = _uiState.asStateFlow()

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
                onSuccess = { categories ->
                    _uiState.update { state ->
                        state.copy(
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
        // 计算总体统计
        calculateTotalStats()
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
     */
    private fun calculateTotalStats() {
        val state = _uiState.value
        var totalCredits = 0.0
        var earnedCredits = 0.0
        var studyingCredits = 0.0
        var totalGradePoints = 0.0
        var totalCreditsForGpa = 0.0

        for (category in state.categories) {
            totalCredits += category.totalCredits
            earnedCredits += category.earnedCredits

            for (course in category.courses) {
                val credits = course.credits.toDoubleOrNull() ?: 0.0

                if (course.studyStatus == StudyStatusUtils.STUDYING) {
                    studyingCredits += credits
                }

                // 计算绩点（只计算已通过的课程）
                if (course.studyStatus == StudyStatusUtils.PASSED && course.gradePoint > 0) {
                    totalGradePoints += course.gradePoint * credits
                    totalCreditsForGpa += credits
                }
            }
        }

        val averageGradePoint = if (totalCreditsForGpa > 0) {
            totalGradePoints / totalCreditsForGpa
        } else 0.0

        _uiState.update {
            it.copy(
                totalCredits = totalCredits,
                earnedCredits = earnedCredits,
                studyingCredits = studyingCredits,
                averageGradePoint = averageGradePoint
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
                onSuccess = { categories ->
                    _uiState.update { state ->
                        state.copy(
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
