package com.suseoaa.projectoaa.presentation.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 成绩响应
 */
@Serializable
data class StudentGradeResponse(
    @SerialName("items")
    val items: List<GradeItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

/**
 * 成绩条目 - 匹配教务系统返回的字段
 */
@Serializable
data class GradeItem(
    @SerialName("bfzcj")
    val bfzcj: String? = "",       // 百分制成绩
    @SerialName("bh")
    val bh: String? = "",          // 班号
    @SerialName("bj")
    val bj: String? = "",          // 班级
    @SerialName("cj")
    val cj: String? = "",          // 成绩
    @SerialName("jd")
    val jd: String? = "",          // 绩点
    @SerialName("jgmc")
    val jgmc: String? = "",        // 学院名称
    @SerialName("jsxm")
    val jsxm: String? = "",        // 教师姓名
    @SerialName("jxbmc")
    val jxbmc: String? = "",       // 教学班名称
    @SerialName("kcbj")
    val kcbj: String? = "",        // 课程标记
    @SerialName("kch")
    val kch: String? = "",         // 课程号
    @SerialName("kclbmc")
    val kclbmc: String? = "",      // 课程类别名称
    @SerialName("kcmc")
    val kcmc: String? = "",        // 课程名称
    @SerialName("kcxzmc")
    val kcxzmc: String? = "",      // 课程性质名称 (专业基础必修等)
    @SerialName("khfsmc")
    val khfsmc: String? = "",      // 考核方式名称
    @SerialName("kkbmmc")
    val kkbmmc: String? = "",      // 开课部门名称
    @SerialName("ksxz")
    val ksxz: String? = "",        // 考试性质 (正常考试/补考)
    @SerialName("njmc")
    val njmc: String? = "",        // 年级名称
    @SerialName("sfxwkc")
    val sfxwkc: String? = "",      // 是否学位课程
    @SerialName("xf")
    val xf: String? = "",          // 学分
    @SerialName("xfjd")
    val xfjd: String? = "",        // 学分绩点
    @SerialName("xh")
    val xh: String? = "",          // 学号
    @SerialName("xm")
    val xm: String? = "",          // 姓名
    @SerialName("xnm")
    val xnm: String? = "",         // 学年码
    @SerialName("xnmmc")
    val xnmmc: String? = "",       // 学年名称
    @SerialName("xqm")
    val xqm: String? = "",         // 学期码
    @SerialName("xqmmc")
    val xqmmc: String? = "",       // 学期名称
    @SerialName("zymc")
    val zymc: String? = ""         // 专业名称
)

data class GradesUiState(
    val isRefreshing: Boolean = false,
    val grades: List<GradeItem> = emptyList(),
    val allGrades: List<GradeItem> = emptyList(),
    val selectedYear: String = "",
    val selectedSemester: String = "3",
    val startYear: Int = 2020,
    val message: String? = null
)

class GradesViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState())
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    init {
        // 初始化当前学年
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
        // 如果在上半年(1-7月)，使用上一年作为学年
        val academicYear = if (currentMonth < 8) currentYear - 1 else currentYear
        
        _uiState.update {
            it.copy(
                selectedYear = academicYear.toString(),
                startYear = academicYear - 4
            )
        }
        
        loadGrades()
    }

    fun loadGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            
            // TODO: 从 SchoolGradeRepository 加载成绩数据
            // 需要实现完整的成绩查询功能
            
            _uiState.update { 
                it.copy(
                    isRefreshing = false,
                    message = "成绩查询功能正在开发中"
                )
            }
        }
    }

    fun refreshGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = "正在连接教务系统...") }
            
            // TODO: 自动登录教务系统并刷新成绩
            
            _uiState.update { 
                it.copy(
                    isRefreshing = false,
                    message = "成绩刷新功能正在开发中"
                )
            }
        }
    }

    fun updateFilter(year: String, semester: String) {
        _uiState.update { state ->
            val filtered = filterGrades(state.allGrades, year, semester)
            state.copy(
                selectedYear = year,
                selectedSemester = semester,
                grades = filtered
            )
        }
    }

    private fun filterGrades(grades: List<GradeItem>, year: String, semester: String): List<GradeItem> {
        return grades.filter { grade ->
            val yearMatch = grade.xnm == year
            val semesterMatch = grade.xqm == semester
            yearMatch && semesterMatch
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
