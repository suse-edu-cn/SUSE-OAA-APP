package com.suseoaa.projectoaa.presentation.teachingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAccountRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.TeachingPlanRepository
import com.suseoaa.projectoaa.util.SchoolAccountPromptManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 以下 UI 状态原本定义在 shared 的 domain/model 里，属于 UI 层反向下沉到领域层。
 * 它们只被本模块的 ViewModel 与 Screen 使用，现归位到 presentation 层。
 */
data class CourseInfoUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val courses: List<CourseInfoItem> = emptyList(),
    val filteredCourses: List<CourseInfoItem> = emptyList(),
    val planId: String = "",
    val totalCount: Int = 0,
    val errorMessage: String? = null,
    val colleges: List<CollegeOption> = emptyList(),
    val majors: List<MajorOption> = emptyList(),
    val grades: List<String> = emptyList(),
    val selectedCollegeId: String = "",
    val selectedMajorId: String = "",
    val selectedGrade: String = "",
    val isLoadingColleges: Boolean = false,
    val isLoadingMajors: Boolean = false,
    val isLoadingPlan: Boolean = false,
    val planInfo: TeachingPlanInfo? = null,
    val selectedYear: String = "",
    val selectedSemester: String = "",
    val searchKeyword: String = "",
    val selectedCourseType: String = "",
    val isFilterExpanded: Boolean = true,
    val isQueryMode: Boolean = false
)

/**
 * 课程信息查询 ViewModel
 * 支持查询任意学院、专业、年级的课程信息
 */
class CourseInfoViewModel(
    private val schoolAccountRepository: SchoolAccountRepository,
    private val teachingPlanRepository: TeachingPlanRepository,
    private val authRepository: SchoolAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseInfoUiState())
    val uiState: StateFlow<CourseInfoUiState> = _uiState.asStateFlow()

    // 当前默认教务账户信息
    private val currentAccount = schoolAccountRepository.observeCurrentAccount()
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
        // 初始化时加载学院列表和年级列表
        loadCollegeList()
        loadGradeList()

        // 监听账户变化，自动加载当前学生的课程信息
        viewModelScope.launch {
            currentAccount.filterNotNull().collect { account ->
                if (_uiState.value.courses.isEmpty()) {
                    val collegeId = account.jgId.orEmpty()
                    val majorId = account.zyhId.orEmpty()
                    val gradeId = account.njdmId

                    _uiState.update {
                        it.copy(
                            selectedCollegeId = collegeId,
                            selectedMajorId = majorId,
                            selectedGrade = gradeId
                        )
                    }

                    if (collegeId.isNotEmpty()) {
                        loadMajorList(collegeId)
                    }

                    // 自动加载当前学生的课程
                    if (collegeId.isNotEmpty() && gradeId.isNotEmpty() && majorId.isNotEmpty()) {
                        loadCourseInfoBySelection(collegeId, gradeId, majorId)
                    }
                }
            }
        }
    }

    private suspend fun ensureSchoolSession(onAccountAdded: () -> Unit): Boolean {
        val account = schoolAccountRepository.getCurrentAccount()
        if (account == null) {
            SchoolAccountPromptManager.requestAddAccount(
                featureName = "课程信息查询",
                onAccountAdded = onAccountAdded
            )
            return false
        }
        val loginResult = authRepository.login(account.studentId, account.password)
        if (loginResult.isFailure) {
            _uiState.update {
                it.copy(
                    errorMessage = loginResult.exceptionOrNull()?.message ?: "教务系统登录失败",
                    isLoading = false,
                    isLoadingColleges = false,
                    isLoadingMajors = false,
                    isLoadingPlan = false
                )
            }
            return false
        }
        return true
    }

    /**
     * 加载学院列表
     */
    private fun loadCollegeList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingColleges = true) }

            if (!ensureSchoolSession { loadCollegeList() }) {
                _uiState.update { it.copy(isLoadingColleges = false) }
                return@launch
            }

            val result = teachingPlanRepository.getCollegeList()
            result.fold(
                onSuccess = { colleges ->
                    _uiState.update { it.copy(colleges = colleges, isLoadingColleges = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "加载学院列表失败: ${error.message}",
                            isLoadingColleges = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 加载年级列表
     */
    private fun loadGradeList() {
        val grades = teachingPlanRepository.generateGradeList()
        _uiState.update { it.copy(grades = grades) }
    }

    /**
     * 加载专业列表
     */
    private fun loadMajorList(collegeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMajors = true, majors = emptyList()) }

            if (!ensureSchoolSession { loadMajorList(collegeId) }) {
                _uiState.update { it.copy(isLoadingMajors = false) }
                return@launch
            }

            val result = teachingPlanRepository.getMajorList(collegeId)
            result.fold(
                onSuccess = { majors ->
                    _uiState.update { it.copy(majors = majors, isLoadingMajors = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "加载专业列表失败: ${error.message}",
                            isLoadingMajors = false
                        )
                    }
                }
            )
        }
    }

    /**
     * 选择学院
     */
    fun selectCollege(collegeId: String) {
        if (collegeId == _uiState.value.selectedCollegeId) return

        _uiState.update {
            it.copy(
                selectedCollegeId = collegeId,
                selectedMajorId = "",  // 重置专业选择
                majors = emptyList(),
                courses = emptyList(),
                filteredCourses = emptyList(),
                planId = "",
                planInfo = null,
                isQueryMode = true
            )
        }

        if (collegeId.isNotEmpty()) {
            loadMajorList(collegeId)
        }
    }

    /**
     * 选择专业
     */
    fun selectMajor(majorId: String) {
        if (majorId == _uiState.value.selectedMajorId) return

        _uiState.update {
            it.copy(
                selectedMajorId = majorId,
                courses = emptyList(),
                filteredCourses = emptyList(),
                planId = "",
                planInfo = null,
                isQueryMode = true
            )
        }

        // 如果三个条件都满足，自动加载课程
        val state = _uiState.value
        if (state.selectedCollegeId.isNotEmpty() &&
            state.selectedGrade.isNotEmpty() &&
            majorId.isNotEmpty()
        ) {
            loadCourseInfoBySelection(state.selectedCollegeId, state.selectedGrade, majorId)
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
                courses = emptyList(),
                filteredCourses = emptyList(),
                planId = "",
                planInfo = null,
                isQueryMode = true
            )
        }

        // 如果三个条件都满足，自动加载课程
        val state = _uiState.value
        if (state.selectedCollegeId.isNotEmpty() &&
            grade.isNotEmpty() &&
            state.selectedMajorId.isNotEmpty()
        ) {
            loadCourseInfoBySelection(state.selectedCollegeId, grade, state.selectedMajorId)
        }
    }

    /**
     * 手动查询课程（点击查询按钮时调用）
     */
    fun queryCourses() {
        viewModelScope.launch {
            if (!ensureSchoolSession {
                    if (_uiState.value.colleges.isEmpty()) loadCollegeList() else queryCourses()
                }
            ) {
                return@launch
            }
            val state = _uiState.value
            if (state.selectedCollegeId.isEmpty() ||
                state.selectedGrade.isEmpty() ||
                state.selectedMajorId.isEmpty()
            ) {
                _uiState.update { it.copy(errorMessage = "请选择完整的查询条件") }
                return@launch
            }
            loadCourseInfoBySelection(
                state.selectedCollegeId,
                state.selectedGrade,
                state.selectedMajorId
            )
        }
    }

    /**
     * 根据选择的学院、年级、专业加载课程信息
     */
    private fun loadCourseInfoBySelection(collegeId: String, gradeId: String, majorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isLoadingPlan = true, errorMessage = null) }

            try {
                if (!ensureSchoolSession { loadCourseInfoBySelection(collegeId, gradeId, majorId) }) {
                    _uiState.update { it.copy(isLoading = false, isLoadingPlan = false) }
                    return@launch
                }

                // 获取培养计划
                val planResult = teachingPlanRepository.getTeachingPlanInfo(
                    collegeId = collegeId,
                    gradeId = gradeId,
                    majorId = majorId
                )

                planResult.fold(
                    onSuccess = { planInfo ->
                        if (planInfo != null) {
                            _uiState.update {
                                it.copy(
                                    planId = planInfo.planId,
                                    planInfo = planInfo,
                                    isLoadingPlan = false
                                )
                            }
                            // 加载课程列表
                            loadCourses(planInfo.planId)
                        } else {
                            _uiState.update {
                                it.copy(
                                    errorMessage = "未找到该专业的培养计划",
                                    isLoading = false,
                                    isLoadingPlan = false
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "获取培养计划失败: ${error.message}",
                                isLoading = false,
                                isLoadingPlan = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "加载失败: ${e.message}",
                        isLoading = false,
                        isLoadingPlan = false
                    )
                }
            }
        }
    }

    /**
     * 加载当前学生的课程信息（快捷入口）
     */
    fun loadStudentCourseInfo() {
        viewModelScope.launch {
            val account = schoolAccountRepository.getCurrentAccount()
            if (account == null) {
                SchoolAccountPromptManager.requestAddAccount(
                    featureName = "课程信息查询",
                    onAccountAdded = { loadStudentCourseInfo() }
                )
                return@launch
            }

            val collegeId = account.jgId.orEmpty()
            val gradeId = account.njdmId
            val majorId = account.zyhId.orEmpty()

            if (collegeId.isEmpty() || gradeId.isEmpty() || majorId.isEmpty()) {
                _uiState.update {
                    it.copy(errorMessage = "学生信息不完整，请先刷新课表或成绩获取完整信息")
                }
                return@launch
            }

            // 更新选择状态
            _uiState.update {
                it.copy(
                    selectedCollegeId = collegeId,
                    selectedMajorId = majorId,
                    selectedGrade = gradeId,
                    isQueryMode = false
                )
            }

            loadMajorList(collegeId)
            loadCourseInfoBySelection(collegeId, gradeId, majorId)
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

    private fun filterCourses(
        courses: List<CourseInfoItem>,
        year: String = "",
        semester: String = "",
        courseType: String = "",
        keyword: String = ""
    ): List<CourseInfoItem> {
        return courses.filter { course ->
            val yearMatch = year.isEmpty() || course.suggestedYear == year
            val semesterMatch = semester.isEmpty() || course.suggestedSemester == semester
            val typeMatch = courseType.isEmpty() || course.courseType == courseType
            val keywordMatch = keyword.isEmpty() ||
                course.courseName.contains(keyword, ignoreCase = true) ||
                course.courseCode.contains(keyword, ignoreCase = true)

            yearMatch && semesterMatch && typeMatch && keywordMatch
        }
    }

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

    fun refresh() {
        val planId = _uiState.value.planId
        if (planId.isNotEmpty()) {
            viewModelScope.launch {
                if (!ensureSchoolSession { refresh() }) return@launch
                _uiState.update { it.copy(isRefreshing = true) }
                loadCourses(planId)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        } else {
            loadStudentCourseInfo()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleFilterExpanded() {
        _uiState.update { it.copy(isFilterExpanded = !it.isFilterExpanded) }
    }

    fun setFilterExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isFilterExpanded = expanded) }
    }

    fun getCoursesBySemester(): Map<String, List<CourseInfoItem>> {
        return teachingPlanRepository.groupCoursesBySemester(_uiState.value.filteredCourses)
    }

    fun getCoursesByType(): Map<String, List<CourseInfoItem>> {
        return teachingPlanRepository.groupCoursesByType(_uiState.value.filteredCourses)
    }

    fun getTotalCredits(): Double {
        return teachingPlanRepository.calculateTotalCredits(_uiState.value.filteredCourses)
    }
}
