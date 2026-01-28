package com.suseoaa.projectoaa.presentation.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.repository.GradeEntity
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.data.repository.SchoolGradeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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
    @SerialName("cjbdczr")
    val cjbdczr: String? = "",     // 成绩变动操作人
    @SerialName("jd")
    val jd: String? = "",          // 绩点
    @SerialName("jg_id")
    val jgId: String? = "",        // 学院ID
    @SerialName("jgmc")
    val jgmc: String? = "",        // 学院名称
    @SerialName("jsxm")
    val jsxm: String? = "",        // 教师姓名
    @SerialName("jxb_id")
    val jxbId: String? = "",       // 教学班ID (用于获取详情)
    @SerialName("jxbmc")
    val jxbmc: String? = "",       // 教学班名称
    @SerialName("kcbj")
    val kcbj: String? = "",        // 课程标记
    @SerialName("kch")
    val kch: String? = "",         // 课程号
    @SerialName("kch_id")
    val kchId: String? = "",       // 课程ID
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
    @SerialName("njdm_id")
    val njdmId: String? = "",      // 年级代码ID
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
    @SerialName("zyh_id")
    val zyhId: String? = "",       // 专业ID
    @SerialName("zymc")
    val zymc: String? = ""         // 专业名称
)

data class GradesUiState(
    val isRefreshing: Boolean = false,
    val grades: List<GradeEntity> = emptyList(),
    val selectedYear: String = "",
    val selectedSemester: String = "3",
    val startYear: Int = 2020,
    val message: String? = null,
    val currentAccount: CourseAccountEntity? = null
)

class GradesViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolGradeRepository: SchoolGradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState())
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    // 当前账户流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount: StateFlow<CourseAccountEntity?> = combine(
        localCourseRepository.getAllAccounts(),
        tokenManager.currentStudentId
    ) { accounts, selectedId ->
        if (accounts.isEmpty()) null
        else accounts.find { it.studentId == selectedId } ?: accounts.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    // 成绩数据流
    @OptIn(ExperimentalCoroutinesApi::class)
    private val gradesFlow: StateFlow<List<GradeEntity>> = combine(
        currentAccount.filterNotNull(),
        _uiState
    ) { account, state ->
        Triple(account.studentId, state.selectedYear, state.selectedSemester)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        schoolGradeRepository.observeGrades(studentId, xnm, xqm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

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

        // 观察当前账户变化
        viewModelScope.launch {
            currentAccount.collect { account ->
                _uiState.update { it.copy(currentAccount = account) }
                // 更新起始年份
                account?.let { acc ->
                    val startYear = acc.njdmId.toIntOrNull() ?: (academicYear - 4)
                    _uiState.update { it.copy(startYear = startYear) }
                }
            }
        }

        // 观察成绩数据变化
        viewModelScope.launch {
            gradesFlow.collect { grades ->
                _uiState.update { it.copy(grades = grades) }
            }
        }
    }

    fun refreshGrades() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch
            _uiState.update { it.copy(isRefreshing = true, message = "正在连接教务系统...") }

            try {
                // 自动重试登录，确保 Session 有效
                val loginResult = schoolAuthRepository.login(account.studentId, account.password)

                if (loginResult.isSuccess) {
                    _uiState.update { it.copy(message = "正在全量同步成绩...") }
                    val result = schoolGradeRepository.fetchAllHistoryGrades(account)

                    result.onSuccess { msg ->
                        _uiState.update { it.copy(message = msg) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(message = "更新失败: ${e.message}") }
                    }
                } else {
                    _uiState.update { it.copy(message = "教务登录失败，请检查密码或网络") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "未知错误: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun updateFilter(year: String, semester: String) {
        _uiState.update { state ->
            state.copy(
                selectedYear = year,
                selectedSemester = semester
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
