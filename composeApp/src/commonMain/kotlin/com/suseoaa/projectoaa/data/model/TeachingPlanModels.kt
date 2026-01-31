package com.suseoaa.projectoaa.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 教学执行计划查询相关模型 ====================

/**
 * 专业原始API响应（从 cxZydmList 接口返回）
 * 包含学院和专业信息
 */
@Serializable
data class MajorApiResponse(
    @SerialName("jg_id") val collegeId: String = "",           // 学院ID
    @SerialName("jgmc") val collegeName: String = "",          // 学院名称
    @SerialName("zyh_id") val majorId: String = "",            // 专业ID
    @SerialName("zymc") val majorName: String = "",            // 专业名称
    @SerialName("zyjc") val majorShortName: String = ""        // 专业简称
)

/**
 * 专业列表响应（用于下拉选择）
 */
@Serializable
data class MajorOption(
    val code: String = "",        // 专业代码
    val name: String = ""         // 专业名称
)

/**
 * 学院列表响应
 */
@Serializable
data class CollegeOption(
    val code: String = "",        // 学院代码
    val name: String = ""         // 学院名称
)

/**
 * 培养计划基础信息（查询修读要求时使用）
 */
@Serializable
data class TeachingPlanInfo(
    @SerialName("jxzxjhxx_id") val planId: String = "",          // 计划ID
    @SerialName("zyh_id") val majorId: String = "",              // 专业ID
    @SerialName("zymc") val majorName: String = "",              // 专业名称
    @SerialName("njdm_id") val gradeId: String = "",             // 年级代码
    @SerialName("jg_id") val collegeId: String = "",             // 学院ID
    @SerialName("jgmc") val collegeName: String = "",            // 学院名称
    @SerialName("xfhj") val totalCredits: String = "",           // 学分合计
    @SerialName("zxshj") val totalHours: String = ""             // 总学时合计
)

/**
 * 培养计划列表响应
 */
@Serializable
data class TeachingPlanListResponse(
    @SerialName("items") val items: List<TeachingPlanInfo> = emptyList(),
    @SerialName("totalResult") val totalResult: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("totalPage") val totalPage: Int = 1
)

/**
 * 修读要求节点信息
 * 对应 API: jxzxjhxfyq_cxJxzxjhxfyqKcxx.html
 */
@Serializable
data class StudyRequirementCourse(
    @SerialName("KCMC") val courseName: String = "",             // 课程名称
    @SerialName("KCH") val courseCode: String = "",              // 课程号
    @SerialName("KCH_ID") val courseId: String = "",             // 课程ID
    @SerialName("XF") val credits: String = "",                  // 学分
    @SerialName("ZXS") val hours: Int = 0,                       // 总学时
    @SerialName("KCXZMC") val courseType: String = "",           // 课程性质名称（专业基础必修、专业选修等）
    @SerialName("KCXZDM") val courseTypeCode: String = "",       // 课程性质代码
    @SerialName("KKBM") val department: String = "",             // 开课部门
    @SerialName("JYXDXNM") val suggestedYear: String = "",       // 建议修读学年（如：2023-2024）
    @SerialName("JYXDXQM") val suggestedSemester: String = "",   // 建议修读学期（1或2）
    @SerialName("XDLX") val studyType: String = "",              // 修读类型
    @SerialName("JCBJ") val isInherited: String = "",            // 继承标记（1=是）
    @SerialName("JCBJMC") val inheritedName: String = "",        // 继承标记名称
    @SerialName("SFMBJC") val ignoreContinue: String = "",       // 是否免必继承
    @SerialName("SFMBYY") val ignoreRequired: String = "",       // 是否免必修要求
    @SerialName("KCKXF") val availableCredits: String = "",      // 可开学分
    @SerialName("XFSFYZ") val creditsSatisfied: String = "",     // 学分是否已足
    @SerialName("SHZT") val auditStatus: String = "",            // 审核状态
    @SerialName("JXZXJHKCXX_ID") val planCourseId: String = ""   // 计划课程ID
)

/**
 * 课程信息（课程信息查询功能用）
 * 对应 API: jxzxjhkcxx_cxJxzxjhkcxxIndex.html
 */
@Serializable
data class CourseInfoItem(
    @SerialName("kcmc") val courseName: String = "",             // 课程名称
    @SerialName("kch") val courseCode: String = "",              // 课程号
    @SerialName("kch_id") val courseId: String = "",             // 课程ID
    @SerialName("xf") val credits: String = "",                  // 学分
    @SerialName("zxs") val hours: Int = 0,                       // 总学时
    @SerialName("kcxzmc") val courseType: String = "",           // 课程性质名称
    @SerialName("kkbmmc") val department: String = "",           // 开课部门
    @SerialName("jyxdxnm") val suggestedYear: String = "",       // 建议修读学年
    @SerialName("jyxdxqm") val suggestedSemester: String = "",   // 建议修读学期
    @SerialName("yyxdxnxqmc") val allowedYearSemester: String = "", // 允许修读学年学期名称
    @SerialName("qsjsz") val weekRange: String = "",             // 起始结束周
    @SerialName("khfsdm") val examMethod: String = "",           // 考核方式（考试/考查）
    @SerialName("xfyqjdmc") val creditRequirementNode: String = "", // 学分要求节点名称
    @SerialName("kclbmc") val courseCategory: String = "",       // 课程类别名称
    @SerialName("zymc") val majorName: String = "",              // 专业名称
    @SerialName("xqmc") val campus: String = "",                 // 校区名称
    @SerialName("xsxxxx") val weeklyHoursInfo: String = "",      // 学时详细信息
    @SerialName("fxzxs") val semesterHoursInfo: String = "",     // 分学期总学时信息
    @SerialName("sflsmc") val implementStatus: String = "",      // 是否落实名称
    @SerialName("shzt") val auditStatus: String = "",            // 审核状态
    @SerialName("jcbj") val isInherited: String = "",            // 继承标记
    @SerialName("jcbjmc") val inheritedName: String = "",        // 继承标记名称
    @SerialName("row_id") val rowId: Int = 0,                    // 行号
    @SerialName("totalresult") val totalResult: Int = 0          // 总结果数
)

/**
 * 课程信息列表响应
 */
@Serializable
data class CourseInfoListResponse(
    @SerialName("items") val items: List<CourseInfoItem> = emptyList(),
    @SerialName("totalResult") val totalResult: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("totalPage") val totalPage: Int = 1,
    @SerialName("showCount") val showCount: Int = 15
)

// ==================== UI 数据模型 ====================

/**
 * 修读要求分类信息（UI层使用）
 */
data class StudyRequirementCategory(
    val categoryName: String,                         // 分类名称
    val categoryCode: String,                         // 分类代码
    val courses: List<StudyRequirementCourse>,        // 课程列表
    val totalCredits: Double,                         // 总学分
    val requiredCredits: Double                       // 要求学分
)

/**
 * 课程信息展示状态
 */
data class CourseInfoUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val courses: List<CourseInfoItem> = emptyList(),
    val filteredCourses: List<CourseInfoItem> = emptyList(),
    val planId: String = "",
    val totalCount: Int = 0,
    val errorMessage: String? = null,
    // 学院/专业/年级筛选
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
    // 课程筛选条件
    val selectedYear: String = "",
    val selectedSemester: String = "",
    val searchKeyword: String = "",
    val selectedCourseType: String = "",
    // UI状态
    val isFilterExpanded: Boolean = true,  // 筛选区域是否展开（默认展开）
    val isQueryMode: Boolean = false       // 是否为自定义查询模式（查其他专业）
)

/**
 * 修读要求展示状态
 */
data class StudyRequirementUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categories: List<StudyRequirementCategory> = emptyList(),
    val selectedGrade: String = "",
    val selectedCollegeId: String = "",
    val selectedMajorId: String = "",
    val grades: List<String> = emptyList(),
    val colleges: List<CollegeOption> = emptyList(),
    val majors: List<MajorOption> = emptyList(),
    val planInfo: TeachingPlanInfo? = null,
    val errorMessage: String? = null,
    // UI状态
    val isFilterExpanded: Boolean = true,           // 筛选区域是否展开
    val expandedCategories: Set<String> = emptySet() // 展开的课程类别名称集合
)

/**
 * 课程类型常量
 */
object CourseTypeConstants {
    const val GENERAL_REQUIRED = "学科基础必修"          // 07
    const val MAJOR_BASE_REQUIRED = "专业基础必修"       // 19
    const val MAJOR_CORE_REQUIRED = "专业核心必修"       // 26
    const val MAJOR_ELECTIVE = "专业选修"               // 33
    const val PRACTICE_REQUIRED = "集中实践必修"         // 24
    const val QUALITY_PRACTICE_REQUIRED = "素质实践必修"  // 25
    const val QUALITY_GENERAL_REQUIRED = "素质通识必修"   // 39
    const val QUALITY_GENERAL_ELECTIVE = "素质通识选修"   // 40
    const val SUBJECT_BASE_ELECTIVE = "学科基础选修"      // 16
    const val COMPOUND_ELECTIVE = "复合培养选修"          // 20
}

/**
 * 学期常量
 */
object SemesterConstants {
    const val FIRST_SEMESTER = "1"
    const val SECOND_SEMESTER = "2"
    
    fun getSemesterName(code: String): String {
        return when (code) {
            FIRST_SEMESTER -> "第一学期"
            SECOND_SEMESTER -> "第二学期"
            else -> "未知"
        }
    }
}

// ==================== 学业情况查询相关模型 ====================

/**
 * 学业情况课程信息（API响应）
 * 对应 API: xsxy/xsxyqk_cxJxzxjhxfyqKcxx.html
 */
@Serializable
data class AcademicStatusCourseItem(
    @SerialName("KCH_ID") val courseId: String = "",              // 课程ID
    @SerialName("KCMC") val courseName: String = "",              // 课程名称
    @SerialName("KCH") val courseCode: String = "",               // 课程号
    @SerialName("XDZT") val studyStatus: String = "",             // 修读状态：1=未修, 2=不及格, 3=在修, 4=已修通过
    @SerialName("XNM") val yearCode: String = "",                 // 学年代码（如2023）
    @SerialName("XNMC") val yearName: String = "",                // 学年名称（如2023-2024）
    @SerialName("XQM") val semesterCode: String = "",             // 学期代码
    @SerialName("XQMMC") val semesterName: String = "",           // 学期名称（1或2）
    @SerialName("CJ") val grade: String = "",                     // 成绩
    @SerialName("MAXCJ") val maxGrade: String = "",               // 最高成绩
    @SerialName("XF") val credits: String = "",                   // 学分
    @SerialName("JD") val gradePoint: Double = 0.0,               // 绩点
    @SerialName("KCXZMC") val courseType: String = "",            // 课程性质名称
    @SerialName("KCLBMC") val courseCategory: String = "",        // 课程类别名称（专业课/公共课/基础课）
    @SerialName("KCLBDM") val courseCategoryCode: String = "",    // 课程类别代码
    @SerialName("XSXXXX") val hoursInfo: String = "",             // 学时详细信息
    @SerialName("SFJHKC") val isPlannedCourse: String = "",       // 是否计划课程（是/否）
    @SerialName("ZYZGKCBJ") val isMajorQualifiedCourse: String = "", // 专业资格课程标记
    @SerialName("JYXDXNM") val suggestedYearCode: String = "",    // 建议修读学年代码
    @SerialName("JYXDXNMC") val suggestedYearName: String = "",   // 建议修读学年名称
    @SerialName("JYXDXQM") val suggestedSemesterCode: String = "", // 建议修读学期代码
    @SerialName("JYXDXQMC") val suggestedSemesterName: String = "", // 建议修读学期名称
    @SerialName("KCZT") val courseStatus: Int = 0,                // 课程状态
    @SerialName("XBX") val electiveType: String = "",             // 选必修类型
    @SerialName("KCZYXXS") val minHours: String = ""              // 课程最小学时
)

/**
 * 学业情况课程类别
 */
data class AcademicStatusCategory(
    val categoryId: String,                              // 类别ID (xfyqjd_id)
    val categoryName: String,                            // 类别名称
    val courses: List<AcademicStatusCourseItem> = emptyList(), // 该类别下的课程
    val isLoading: Boolean = false,                      // 是否正在加载
    val isLoaded: Boolean = false,                       // 是否已加载
    val totalCredits: Double = 0.0,                      // 总学分
    val earnedCredits: Double = 0.0,                     // 已获学分
    val passedCount: Int = 0,                            // 已通过课程数
    val failedCount: Int = 0,                            // 不及格课程数
    val studyingCount: Int = 0,                          // 在修课程数
    val notStudiedCount: Int = 0                         // 未修课程数
)

/**
 * 学业情况UI状态
 */
data class AcademicStatusUiState(
    val isLoading: Boolean = false,                      // 主页加载状态
    val isRefreshing: Boolean = false,                   // 刷新状态
    val categories: List<AcademicStatusCategory> = emptyList(), // 课程类别列表
    val expandedCategories: Set<String> = emptySet(),    // 展开的类别ID集合
    val selectedFilter: AcademicStatusFilter = AcademicStatusFilter.ALL, // 筛选条件
    val errorMessage: String? = null,
    // 统计数据
    val totalCredits: Double = 0.0,                      // 应修总学分
    val earnedCredits: Double = 0.0,                     // 已获学分
    val studyingCredits: Double = 0.0,                   // 在修学分
    val averageGradePoint: Double = 0.0                  // 平均绩点
)

/**
 * 学业情况筛选器
 */
enum class AcademicStatusFilter(val displayName: String) {
    ALL("全部"),
    PASSED("已通过"),
    FAILED("不及格"),
    STUDYING("在修"),
    NOT_STUDIED("未修")
}

/**
 * 修读状态工具类
 */
object StudyStatusUtils {
    const val NOT_STUDIED = "1"      // 未修
    const val FAILED = "2"           // 不及格
    const val STUDYING = "3"         // 在修
    const val PASSED = "4"           // 已修通过

    fun getStatusName(code: String): String {
        return when (code) {
            NOT_STUDIED -> "未修"
            FAILED -> "不及格"
            STUDYING -> "在修"
            PASSED -> "已通过"
            else -> "未知"
        }
    }

    fun matchesFilter(statusCode: String, filter: AcademicStatusFilter): Boolean {
        return when (filter) {
            AcademicStatusFilter.ALL -> true
            AcademicStatusFilter.PASSED -> statusCode == PASSED
            AcademicStatusFilter.FAILED -> statusCode == FAILED
            AcademicStatusFilter.STUDYING -> statusCode == STUDYING
            AcademicStatusFilter.NOT_STUDIED -> statusCode == NOT_STUDIED
        }
    }
}
