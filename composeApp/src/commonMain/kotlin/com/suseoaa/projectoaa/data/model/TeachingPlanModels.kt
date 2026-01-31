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
    // 筛选条件
    val selectedYear: String = "",
    val selectedSemester: String = "",
    val searchKeyword: String = "",
    val selectedCourseType: String = "",
    // UI状态
    val isFilterExpanded: Boolean = false  // 筛选区域是否展开
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
