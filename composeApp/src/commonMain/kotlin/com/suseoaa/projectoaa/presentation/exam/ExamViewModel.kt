package com.suseoaa.projectoaa.presentation.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.data.repository.ExamCacheEntity
import com.suseoaa.projectoaa.util.getCurrentTerm
import com.suseoaa.projectoaa.util.parseExamTimeRange
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 考试响应 - 匹配教务系统返回的完整结构
 */
@Serializable
data class ExamApiResponse(
    @SerialName("items")
    val items: List<ExamApiItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1,
    @SerialName("totalPage")
    val totalPage: Int? = 1
)

/**
 * 考试信息条目 - 匹配教务系统返回的完整字段
 */
@Serializable
data class ExamApiItem(
    @SerialName("kcmc")
    val kcmc: String? = "",       // 课程名称: "网络安全技术"
    @SerialName("kssj")
    val kssj: String? = "",       // 考试时间: "2026-01-08(09:30-11:30)"
    @SerialName("cdmc")
    val cdmc: String? = "",       // 教室名称: "LA5-322"
    @SerialName("cdxqmc")
    val cdxqmc: String? = "",     // 校区: "临港校区"
    @SerialName("ksmc")
    val ksmc: String? = "",       // 考试名称: "2025-2026-1 期末考试"
    @SerialName("xnm")
    val xnm: String? = "",        // 学年码: "2025"
    @SerialName("xnmc")
    val xnmc: String? = "",       // 学年名称: "2025-2026"
    @SerialName("xqm")
    val xqm: String? = "",        // 学期码: "3"
    @SerialName("xqmmc")
    val xqmmc: String? = "",      // 学期名称: "1"
    @SerialName("khfs")
    val khfs: String? = "",       // 考核方式: "考试"
    @SerialName("xf")
    val xf: String? = "",         // 学分: "3.0"
    @SerialName("kkxy")
    val kkxy: String? = "",       // 开课学院
    @SerialName("kch")
    val kch: String? = "",        // 课程号
    @SerialName("bj")
    val bj: String? = "",         // 班级
    @SerialName("xh")
    val xh: String? = "",         // 学号
    @SerialName("xm")
    val xm: String? = ""          // 姓名
)

/**
 * 考试 UI 状态
 */
data class ExamUiItem(
    val id: Long = 0,              // 数据库 ID，用于编辑和删除
    val courseName: String,
    val examName: String,
    val time: String,
    val location: String,
    val credit: String,
    val examType: String,
    val yearSemester: String,      // 学年学期: "2025-2026 第1学期"
    val isEnded: Boolean = false,  // 是否已结束
    val isCustom: Boolean = false  // 是否为用户自定义
)

/**
 * 学期选项
 */
data class SemesterOption(
    val year: String,              // 学年码: "2025"
    val semester: String,          // 学期码: "3" / "12"
    val displayName: String        // 显示名称: "2025-2026 第1学期"
)

/**
 * 考试页面 UI 状态
 */
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
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolInfoRepository: SchoolInfoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    // 当前账户流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
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
     * 根据用户年级生成学期选项列表
     * @param njdmId 年级代码，如 "2023" 表示2023级
     */
    private fun generateSemesterOptions(njdmId: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentYear = now.year
        val currentMonth = now.monthNumber
        
        // 解析入学年份（年级代码通常就是入学年份）
        val enrollmentYear = njdmId.toIntOrNull() ?: (currentYear - 4)
        
        val semesters = mutableListOf<SemesterOption>()
        
        // 确定当前所在的学年和学期
        // 学年规则：8月-次年1月为第一学期，2月-7月为第二学期
        // 当前学年的起始年份
        val currentAcademicYear = if (currentMonth >= 8) currentYear else currentYear - 1
        // 当前学期：8-1月是第一学期(3)，2-7月是第二学期(12)
        val currentSemesterCode = if (currentMonth >= 8 || currentMonth <= 1) "3" else "12"
        
        // 从当前学年往前推到入学年份
        for (academicYear in currentAcademicYear downTo enrollmentYear) {
            // 判断该学年的两个学期是否应该显示
            val isCurrentAcademicYear = (academicYear == currentAcademicYear)
            
            // 第一学期（学年秋季学期）
            // 对于当前学年，需要检查是否已经到了第一学期
            val showFirstSemester = !isCurrentAcademicYear || currentMonth >= 8 || currentMonth <= 1 || currentSemesterCode == "12"
            
            // 第二学期（学年春季学期）
            // 对于当前学年，如果当前是第一学期，则第二学期还没开始
            val showSecondSemester = if (isCurrentAcademicYear) {
                currentSemesterCode == "12" // 只有当前是第二学期时才显示当前学年的第二学期
            } else {
                true // 往年的两个学期都显示
            }
            
            if (showFirstSemester) {
                semesters.add(SemesterOption(
                    year = academicYear.toString(),
                    semester = "3",
                    displayName = "${academicYear}-${academicYear + 1} 第1学期"
                ))
            }
            
            if (showSecondSemester && academicYear >= enrollmentYear) {
                semesters.add(SemesterOption(
                    year = academicYear.toString(),
                    semester = "12",
                    displayName = "${academicYear}-${academicYear + 1} 第2学期"
                ))
            }
        }
        
        // 按学年降序、学期降序排序（最新的在前面）
        val sortedSemesters = semesters.sortedWith { a, b ->
            val yearCompare = b.year.compareTo(a.year)
            if (yearCompare != 0) yearCompare
            else b.semester.compareTo(a.semester) // "3" < "12"，所以第二学期会排在前面
        }
        
        _uiState.update { it.copy(availableSemesters = sortedSemesters) }
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
     * 处理自定义考试项
     */
    private fun processCustomExams(exams: List<ExamCacheEntity>): List<ExamUiItem> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        
        return exams.map { exam ->
            val timeRange = parseExamTimeRange(exam.time)
            val isEnded = if (timeRange != null) now > timeRange.second else false
            
            ExamUiItem(
                id = exam.id,
                courseName = exam.courseName,
                examName = exam.examName,
                time = exam.time,
                location = exam.location,
                credit = exam.credit,
                examType = exam.examType,
                yearSemester = exam.yearSemester,
                isEnded = isEnded,
                isCustom = true
            )
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
     * 处理考试项，转换为 UI 状态并排序
     */
    private fun processExamItems(items: List<ExamApiItem>): List<ExamUiItem> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        
        return items.map { item ->
            val timeRange = parseExamTimeRange(item.kssj ?: "")
            val isEnded = if (timeRange != null) now > timeRange.second else false
            
            var location = item.cdmc ?: "地点待定"
            if (!item.cdxqmc.isNullOrBlank()) {
                location += "(${item.cdxqmc})"
            }
            
            val semesterName = when (item.xqm) {
                "3" -> "第1学期"
                "12" -> "第2学期"
                "16" -> "第3学期"
                else -> "第${item.xqmmc ?: "?"}学期"
            }
            
            ExamUiItem(
                id = 0, // API 返回的没有本地 ID
                courseName = item.kcmc ?: "未知课程",
                examName = item.ksmc ?: "",
                time = item.kssj ?: "时间待定",
                location = location,
                credit = item.xf ?: "",
                examType = item.khfs ?: "考试",
                yearSemester = "${item.xnmc ?: ""} $semesterName",
                isEnded = isEnded,
                isCustom = false
            )
        }.sortedWith { a, b ->
            // 已结束的排后面，未结束的按时间升序
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
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "删除失败") }
            }
        }
    }
}
