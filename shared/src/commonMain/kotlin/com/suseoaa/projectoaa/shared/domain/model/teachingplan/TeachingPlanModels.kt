package com.suseoaa.projectoaa.shared.domain.model.teachingplan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 教学执行计划查询相关模型 ====================

@Serializable
data class MajorApiResponse(
    @SerialName("jg_id") val collegeId: String = "",
    @SerialName("jgmc") val collegeName: String = "",
    @SerialName("zyh_id") val majorId: String = "",
    @SerialName("zymc") val majorName: String = "",
    @SerialName("zyjc") val majorShortName: String = ""
)

@Serializable
data class MajorOption(
    val code: String = "",
    val name: String = ""
)

@Serializable
data class CollegeOption(
    val code: String = "",
    val name: String = ""
)

@Serializable
data class TeachingPlanInfo(
    @SerialName("jxzxjhxx_id") val planId: String = "",
    @SerialName("zyh_id") val majorId: String = "",
    @SerialName("zymc") val majorName: String = "",
    @SerialName("njdm_id") val gradeId: String = "",
    @SerialName("jg_id") val collegeId: String = "",
    @SerialName("jgmc") val collegeName: String = "",
    @SerialName("xfhj") val totalCredits: String = "",
    @SerialName("zxshj") val totalHours: String = ""
)

@Serializable
data class TeachingPlanListResponse(
    @SerialName("items") val items: List<TeachingPlanInfo> = emptyList(),
    @SerialName("totalResult") val totalResult: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("totalPage") val totalPage: Int = 1
)

@Serializable
data class StudyRequirementCourse(
    @SerialName("KCMC") val courseName: String = "",
    @SerialName("KCH") val courseCode: String = "",
    @SerialName("KCH_ID") val courseId: String = "",
    @SerialName("XF") val credits: String = "",
    @SerialName("ZXS") val hours: Int = 0,
    @SerialName("KCXZMC") val courseType: String = "",
    @SerialName("KCXZDM") val courseTypeCode: String = "",
    @SerialName("KKBM") val department: String = "",
    @SerialName("JYXDXNM") val suggestedYear: String = "",
    @SerialName("JYXDXQM") val suggestedSemester: String = "",
    @SerialName("XDLX") val studyType: String = "",
    @SerialName("JCBJ") val isInherited: String = "",
    @SerialName("JCBJMC") val inheritedName: String = "",
    @SerialName("SFMBJC") val ignoreContinue: String = "",
    @SerialName("SFMBYY") val ignoreRequired: String = "",
    @SerialName("KCKXF") val availableCredits: String = "",
    @SerialName("XFSFYZ") val creditsSatisfied: String = "",
    @SerialName("SHZT") val auditStatus: String = "",
    @SerialName("JXZXJHKCXX_ID") val planCourseId: String = ""
)

@Serializable
data class CourseInfoItem(
    @SerialName("kcmc") val courseName: String = "",
    @SerialName("kch") val courseCode: String = "",
    @SerialName("kch_id") val courseId: String = "",
    @SerialName("xf") val credits: String = "",
    @SerialName("zxs") val hours: Int = 0,
    @SerialName("kcxzmc") val courseType: String = "",
    @SerialName("kkbmmc") val department: String = "",
    @SerialName("jyxdxnm") val suggestedYear: String = "",
    @SerialName("jyxdxqm") val suggestedSemester: String = "",
    @SerialName("yyxdxnxqmc") val allowedYearSemester: String = "",
    @SerialName("qsjsz") val weekRange: String = "",
    @SerialName("khfsdm") val examMethod: String = "",
    @SerialName("xfyqjdmc") val creditRequirementNode: String = "",
    @SerialName("kclbmc") val courseCategory: String = "",
    @SerialName("zymc") val majorName: String = "",
    @SerialName("xqmc") val campus: String = "",
    @SerialName("xsxxxx") val weeklyHoursInfo: String = "",
    @SerialName("fxzxs") val semesterHoursInfo: String = "",
    @SerialName("sflsmc") val implementStatus: String = "",
    @SerialName("shzt") val auditStatus: String = "",
    @SerialName("jcbj") val isInherited: String = "",
    @SerialName("jcbjmc") val inheritedName: String = "",
    @SerialName("row_id") val rowId: Int = 0,
    @SerialName("totalresult") val totalResult: Int = 0
)

@Serializable
data class CourseInfoListResponse(
    @SerialName("items") val items: List<CourseInfoItem> = emptyList(),
    @SerialName("totalResult") val totalResult: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("totalPage") val totalPage: Int = 1,
    @SerialName("showCount") val showCount: Int = 15
)

// ==================== UI 数据模型 ====================

data class StudyRequirementCategory(
    val categoryName: String,
    val categoryCode: String,
    val courses: List<StudyRequirementCourse>,
    val totalCredits: Double,
    val requiredCredits: Double
)

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
    val isFilterExpanded: Boolean = true,
    val expandedCategories: Set<String> = emptySet()
)

object CourseTypeConstants {
    const val GENERAL_REQUIRED = "学科基础必修"
    const val MAJOR_BASE_REQUIRED = "专业基础必修"
    const val MAJOR_CORE_REQUIRED = "专业核心必修"
    const val MAJOR_ELECTIVE = "专业选修"
    const val PRACTICE_REQUIRED = "集中实践必修"
    const val QUALITY_PRACTICE_REQUIRED = "素质实践必修"
    const val QUALITY_GENERAL_REQUIRED = "素质通识必修"
    const val QUALITY_GENERAL_ELECTIVE = "素质通识选修"
    const val SUBJECT_BASE_ELECTIVE = "学科基础选修"
    const val COMPOUND_ELECTIVE = "复合培养选修"
}

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

@Serializable
data class AcademicStatusCourseItem(
    @SerialName("KCH_ID") val courseId: String = "",
    @SerialName("KCMC") val courseName: String = "",
    @SerialName("KCH") val courseCode: String = "",
    @SerialName("XDZT") val studyStatus: String = "",
    @SerialName("XNM") val yearCode: String = "",
    @SerialName("XNMC") val yearName: String = "",
    @SerialName("XQM") val semesterCode: String = "",
    @SerialName("XQMMC") val semesterName: String = "",
    @SerialName("CJ") val grade: String = "",
    @SerialName("MAXCJ") val maxGrade: String = "",
    @SerialName("XF") val credits: String = "",
    @SerialName("JD") val gradePoint: Double = 0.0,
    @SerialName("KCXZMC") val courseType: String = "",
    @SerialName("KCLBMC") val courseCategory: String = "",
    @SerialName("KCLBDM") val courseCategoryCode: String = "",
    @SerialName("XSXXXX") val hoursInfo: String = "",
    @SerialName("SFJHKC") val isPlannedCourse: String = "",
    @SerialName("ZYZGKCBJ") val isMajorQualifiedCourse: String = "",
    @SerialName("JYXDXNM") val suggestedYearCode: String = "",
    @SerialName("JYXDXNMC") val suggestedYearName: String = "",
    @SerialName("JYXDXQM") val suggestedSemesterCode: String = "",
    @SerialName("JYXDXQMC") val suggestedSemesterName: String = "",
    @SerialName("KCZT") val courseStatus: Int = 0,
    @SerialName("XBX") val electiveType: String = "",
    @SerialName("KCZYXXS") val minHours: String = ""
)

data class AcademicStatusCategory(
    val categoryId: String,
    val categoryName: String,
    val courses: List<AcademicStatusCourseItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val totalCredits: Double = 0.0,
    val earnedCredits: Double = 0.0,
    val passedCount: Int = 0,
    val failedCount: Int = 0,
    val studyingCount: Int = 0,
    val notStudiedCount: Int = 0,
    // 以下为从教务系统HTML解析出的原始要求数据
    val requiredCredits: Double = 0.0, // 要求最低学分 (yqzdxf)
    val systemEarnedCredits: Double = 0.0, // 教务系统已获学分 (yxxf)
    val isPassed: Boolean = false, // 节点是否通过 (sftg)
    val jdkcsx: String = "", // 节点课程属性: 1=课程, 2=课程类别, 3=课程归属, 4=课程组
    val parentId: String = "", // 父节点ID
    val childRelation: String = "" // 子节点关系: 1=并且, 0=或者
)

/**
 * 教学计划总体信息（从HTML根节点解析）
 */
data class AcademicPlanOverview(
    val planName: String = "", // 如 "2023网络工程"
    val totalRequiredCredits: Double = 0.0, // 毕业要求总学分
    val totalEarnedCredits: Double = 0.0, // 已获总学分
    val totalRemainingCredits: Double = 0.0, // 未获得学分
    val isPassed: Boolean = false // 是否全部通过
)

data class AcademicStatusUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categories: List<AcademicStatusCategory> = emptyList(),
    val expandedCategories: Set<String> = emptySet(),
    val selectedFilter: AcademicStatusFilter = AcademicStatusFilter.ALL,
    val errorMessage: String? = null,
    val totalCredits: Double = 0.0,
    val earnedCredits: Double = 0.0,
    val studyingCredits: Double = 0.0,
    val averageGradePoint: Double = 0.0,
    // 教务系统原始的总体学分要求
    val planOverview: AcademicPlanOverview = AcademicPlanOverview(),
    // 其它课程学分要求的课程（qtkcxfyq节点）
    val otherCourses: List<AcademicStatusCourseItem> = emptyList(),
    val otherCoursesPassedCount: Int = 0,
    val otherCoursesTotalCount: Int = 0,
    // 计划内课程统计
    val planTotalCourses: Int = 0,
    val planPassedCount: Int = 0,
    val planFailedCount: Int = 0,
    val planStudyingCount: Int = 0,
    val planNotStudiedCount: Int = 0,
    // 计划外课程统计
    val nonPlanCourses: List<AcademicStatusCourseItem> = emptyList(),
    val nonPlanPassedCount: Int = 0,
    val nonPlanFailedCount: Int = 0
)

enum class AcademicStatusFilter(val displayName: String) {
    ALL("全部"),
    PASSED("已通过"),
    FAILED("不及格"),
    STUDYING("在修"),
    NOT_STUDIED("未修")
}

object StudyStatusUtils {
    // XDZT字段: 1=在修, 2=不及格, 3=未修, 4=已通过
    const val STUDYING = "1"
    const val FAILED = "2"
    const val NOT_STUDIED = "3"
    const val PASSED = "4"

    fun getStatusName(code: String): String {
        return when (code) {
            STUDYING -> "在修"
            FAILED -> "不及格"
            NOT_STUDIED -> "未修"
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
