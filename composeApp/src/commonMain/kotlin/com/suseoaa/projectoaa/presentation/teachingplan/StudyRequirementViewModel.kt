package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.data.repository.TeachingPlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 修读要求查询 ViewModel
 * 可以查看任意专业、任意年级的修读要求
 */
class StudyRequirementViewModel(
    private val teachingPlanRepository: TeachingPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyRequirementUiState())
    val uiState: StateFlow<StudyRequirementUiState> = _uiState.asStateFlow()

    init {
        // 初始化年级列表
        _uiState.update { 
            it.copy(grades = teachingPlanRepository.generateGradeList())
        }
        // 加载学院列表
        loadColleges()
    }

    /**
     * 加载学院列表
     */
    private fun loadColleges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = teachingPlanRepository.getCollegeList()
            result.fold(
                onSuccess = { colleges ->
                    _uiState.update { 
                        it.copy(
                            colleges = colleges,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            errorMessage = "加载学院列表失败: ${error.message}",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 选择学院，加载对应专业列表
     */
    fun selectCollege(collegeId: String) {
        if (collegeId == _uiState.value.selectedCollegeId) return
        
        _uiState.update { 
            it.copy(
                selectedCollegeId = collegeId,
                selectedMajorId = "",
                majors = emptyList(),
                categories = emptyList(),
                planInfo = null
            )
        }
        
        if (collegeId.isNotEmpty()) {
            loadMajors(collegeId)
        }
    }

    /**
     * 加载专业列表
     */
    private fun loadMajors(collegeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = teachingPlanRepository.getMajorList(collegeId)
            result.fold(
                onSuccess = { majors ->
                    _uiState.update { 
                        it.copy(
                            majors = majors,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            errorMessage = "加载专业列表失败: ${error.message}",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 选择年级
     */
    fun selectGrade(grade: String) {
        if (grade == _uiState.value.selectedGrade) return
        _uiState.update { 
            it.copy(
                selectedGrade = grade,
                categories = emptyList(),
                planInfo = null
            )
        }
        // 检查是否可以查询
        checkAndQuery()
    }

    /**
     * 选择专业
     */
    fun selectMajor(majorId: String) {
        if (majorId == _uiState.value.selectedMajorId) return
        _uiState.update { 
            it.copy(
                selectedMajorId = majorId,
                categories = emptyList(),
                planInfo = null
            )
        }
        // 检查是否可以查询
        checkAndQuery()
    }

    /**
     * 检查条件并查询
     */
    private fun checkAndQuery() {
        val state = _uiState.value
        if (state.selectedGrade.isNotEmpty() && 
            state.selectedCollegeId.isNotEmpty() && 
            state.selectedMajorId.isNotEmpty()) {
            queryStudyRequirements()
        }
    }

    /**
     * 查询修读要求
     */
    fun queryStudyRequirements() {
        val state = _uiState.value
        if (state.selectedGrade.isEmpty() || 
            state.selectedCollegeId.isEmpty() || 
            state.selectedMajorId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "请选择完整的查询条件") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // 1. 获取培养计划信息
                val planResult = teachingPlanRepository.getTeachingPlanInfo(
                    collegeId = state.selectedCollegeId,
                    gradeId = state.selectedGrade,
                    majorId = state.selectedMajorId
                )
                
                planResult.fold(
                    onSuccess = { planInfo ->
                        if (planInfo != null) {
                            _uiState.update { it.copy(planInfo = planInfo) }
                            // 2. 获取课程列表
                            loadCoursesByPlan(planInfo.planId)
                        } else {
                            _uiState.update { 
                                it.copy(
                                    errorMessage = "未找到该专业的培养计划",
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                errorMessage = "查询失败: ${error.message}",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "查询失败: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 根据培养计划加载课程
     */
    private suspend fun loadCoursesByPlan(planId: String) {
        val result = teachingPlanRepository.getCourseInfoList(planId)
        result.fold(
            onSuccess = { response ->
                // 按课程类型分组
                val grouped = teachingPlanRepository.groupCoursesByType(response.items)
                val categories = grouped.map { (type, courses) ->
                    StudyRequirementCategory(
                        categoryName = type,
                        categoryCode = courses.firstOrNull()?.courseType ?: "",
                        courses = courses.map { course ->
                            StudyRequirementCourse(
                                courseName = course.courseName,
                                courseCode = course.courseCode,
                                courseId = course.courseId,
                                credits = course.credits,
                                hours = course.hours,
                                courseType = course.courseType,
                                department = course.department,
                                suggestedYear = course.suggestedYear,
                                suggestedSemester = course.suggestedSemester
                            )
                        },
                        totalCredits = courses.sumOf { it.credits.toDoubleOrNull() ?: 0.0 },
                        requiredCredits = 0.0 // 实际要求需要从其他接口获取
                    )
                }.sortedBy { 
                    // 按课程类型排序
                    when {
                        it.categoryName.contains("基础必修") -> 0
                        it.categoryName.contains("核心必修") -> 1
                        it.categoryName.contains("选修") -> 2
                        it.categoryName.contains("实践") -> 3
                        it.categoryName.contains("通识") -> 4
                        else -> 5
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        isLoading = false,
                        // 新数据加载后默认全部展开
                        expandedCategories = categories.map { it.categoryName }.toSet()
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { 
                    it.copy(
                        errorMessage = "加载课程失败: ${error.message}",
                        isLoading = false
                    )
                }
            }
        )
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        val state = _uiState.value
        if (state.planInfo != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                loadCoursesByPlan(state.planInfo!!.planId)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        } else if (state.selectedGrade.isNotEmpty() && 
                   state.selectedCollegeId.isNotEmpty() && 
                   state.selectedMajorId.isNotEmpty()) {
            queryStudyRequirements()
        } else {
            loadColleges()
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 切换筛选区域展开状态
     */
    fun toggleFilterExpanded() {
        _uiState.update { it.copy(isFilterExpanded = !it.isFilterExpanded) }
    }

    /**
     * 设置筛选区域展开状态
     */
    fun setFilterExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isFilterExpanded = expanded) }
    }

    /**
     * 切换类别展开状态
     */
    fun toggleCategoryExpanded(categoryName: String) {
        _uiState.update { state ->
            val newSet = if (state.expandedCategories.contains(categoryName)) {
                state.expandedCategories - categoryName
            } else {
                state.expandedCategories + categoryName
            }
            state.copy(expandedCategories = newSet)
        }
    }

    /**
     * 检查类别是否展开
     */
    fun isCategoryExpanded(categoryName: String): Boolean {
        return _uiState.value.expandedCategories.contains(categoryName)
    }

    /**
     * 全部展开
     */
    fun expandAllCategories() {
        _uiState.update { state ->
            state.copy(expandedCategories = state.categories.map { it.categoryName }.toSet())
        }
    }

    /**
     * 全部折叠
     */
    fun collapseAllCategories() {
        _uiState.update { it.copy(expandedCategories = emptySet()) }
    }
}
