package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.*
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.TeachingPlanRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 课程信息查询 ViewModel
 * 只能查询当前学生自己专业的课程信息
 */
class CourseInfoViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val teachingPlanRepository: TeachingPlanRepository,
    private val authRepository: SchoolAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseInfoUiState())
    val uiState: StateFlow<CourseInfoUiState> = _uiState.asStateFlow()

    // 当前账户信息
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(localCourseRepository.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 可用的学年列表（从课程中提取）
    val availableYears: StateFlow<List<String>> = _uiState.map { state ->
        state.courses
            .map { it.suggestedYear }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 可用的课程类型列表
    val availableCourseTypes: StateFlow<List<String>> = _uiState.map { state ->
        state.courses
            .map { it.courseType }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // 监听账户变化，自动加载数据
        viewModelScope.launch {
            currentAccount.collect { account ->
                if (account != null && _uiState.value.courses.isEmpty()) {
                    loadStudentCourseInfo()
                }
            }
        }
    }

    /**
     * 加载当前学生的课程信息
     */
    fun loadStudentCourseInfo() {
        val account = currentAccount.value
        if (account == null) {
            _uiState.update { it.copy(errorMessage = "请先登录") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // 1. 确保已登录
                authRepository.login(account.studentId, account.password)
                
                // 2. 获取学生信息（包含专业、年级等）
                val collegeId = account.jgId ?: ""
                val gradeId = account.njdmId
                val majorId = account.zyhId ?: ""
                
                if (collegeId.isEmpty() || gradeId.isEmpty() || majorId.isEmpty()) {
                    _uiState.update { 
                        it.copy(
                            errorMessage = "学生信息不完整，请重新登录获取完整信息",
                            isLoading = false
                        )
                    }
                    return@launch
                }
                
                // 3. 获取培养计划
                val planResult = teachingPlanRepository.getTeachingPlanInfo(
                    collegeId = collegeId,
                    gradeId = gradeId,
                    majorId = majorId
                )
                
                planResult.fold(
                    onSuccess = { planInfo ->
                        if (planInfo != null) {
                            _uiState.update { it.copy(planId = planInfo.planId) }
                            // 4. 获取课程列表
                            loadCourses(planInfo.planId)
                        } else {
                            _uiState.update { 
                                it.copy(
                                    errorMessage = "未找到您的培养计划",
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                errorMessage = "获取培养计划失败: ${error.message}",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "加载失败: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 加载课程列表
     */
    private suspend fun loadCourses(planId: String) {
        val state = _uiState.value
        val result = teachingPlanRepository.getCourseInfoList(
            planId = planId,
            suggestedYear = state.selectedYear,
            suggestedSemester = state.selectedSemester,
            studyType = if (state.selectedCourseType.isNotEmpty()) "zx" else ""
        )
        
        result.fold(
            onSuccess = { response ->
                val courses = response.items
                _uiState.update { currentState ->
                    currentState.copy(
                        courses = courses,
                        filteredCourses = filterCourses(
                            courses = courses,
                            year = currentState.selectedYear,
                            semester = currentState.selectedSemester,
                            courseType = currentState.selectedCourseType,
                            keyword = currentState.searchKeyword
                        ),
                        totalCount = response.totalResult,
                        isLoading = false
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
     * 筛选课程
     * @param courses 待筛选的课程列表
     * @param year 学年筛选条件（可选）
     * @param semester 学期筛选条件（可选）
     * @param courseType 课程类型筛选条件（可选）
     * @param keyword 关键字搜索条件（可选）
     */
    private fun filterCourses(
        courses: List<CourseInfoItem>,
        year: String = "",
        semester: String = "",
        courseType: String = "",
        keyword: String = ""
    ): List<CourseInfoItem> {
        return courses.filter { course ->
            // 按学年筛选
            val yearMatch = year.isEmpty() || 
                            course.suggestedYear == year
            // 按学期筛选
            val semesterMatch = semester.isEmpty() || 
                                course.suggestedSemester == semester
            // 按课程类型筛选
            val typeMatch = courseType.isEmpty() || 
                            course.courseType == courseType
            // 按关键字搜索
            val keywordMatch = keyword.isEmpty() || 
                               course.courseName.contains(keyword, ignoreCase = true) ||
                               course.courseCode.contains(keyword, ignoreCase = true)
            
            yearMatch && semesterMatch && typeMatch && keywordMatch
        }
    }

    /**
     * 设置学年筛选
     */
    fun setYearFilter(year: String) {
        _uiState.update { state ->
            state.copy(
                selectedYear = year,
                filteredCourses = filterCourses(
                    courses = state.courses,
                    year = year,
                    semester = state.selectedSemester,
                    courseType = state.selectedCourseType,
                    keyword = state.searchKeyword
                )
            )
        }
    }

    /**
     * 设置学期筛选
     */
    fun setSemesterFilter(semester: String) {
        _uiState.update { state ->
            state.copy(
                selectedSemester = semester,
                filteredCourses = filterCourses(
                    courses = state.courses,
                    year = state.selectedYear,
                    semester = semester,
                    courseType = state.selectedCourseType,
                    keyword = state.searchKeyword
                )
            )
        }
    }

    /**
     * 设置课程类型筛选
     */
    fun setCourseTypeFilter(courseType: String) {
        _uiState.update { state ->
            state.copy(
                selectedCourseType = courseType,
                filteredCourses = filterCourses(
                    courses = state.courses,
                    year = state.selectedYear,
                    semester = state.selectedSemester,
                    courseType = courseType,
                    keyword = state.searchKeyword
                )
            )
        }
    }

    /**
     * 设置搜索关键字
     */
    fun setSearchKeyword(keyword: String) {
        _uiState.update { state ->
            state.copy(
                searchKeyword = keyword,
                filteredCourses = filterCourses(
                    courses = state.courses,
                    year = state.selectedYear,
                    semester = state.selectedSemester,
                    courseType = state.selectedCourseType,
                    keyword = keyword
                )
            )
        }
    }

    /**
     * 清除所有筛选条件
     */
    fun clearFilters() {
        _uiState.update { state ->
            state.copy(
                selectedYear = "",
                selectedSemester = "",
                selectedCourseType = "",
                searchKeyword = "",
                filteredCourses = state.courses
            )
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        val planId = _uiState.value.planId
        if (planId.isNotEmpty()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                loadCourses(planId)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        } else {
            loadStudentCourseInfo()
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
     * 获取按学期分组的课程
     */
    fun getCoursesBySemester(): Map<String, List<CourseInfoItem>> {
        return teachingPlanRepository.groupCoursesBySemester(_uiState.value.filteredCourses)
    }

    /**
     * 获取按类型分组的课程
     */
    fun getCoursesByType(): Map<String, List<CourseInfoItem>> {
        return teachingPlanRepository.groupCoursesByType(_uiState.value.filteredCourses)
    }

    /**
     * 计算当前筛选结果的总学分
     */
    fun getTotalCredits(): Double {
        return teachingPlanRepository.calculateTotalCredits(_uiState.value.filteredCourses)
    }
}
