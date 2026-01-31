package com.suseoaa.projectoaa.data.repository

import com.suseoaa.projectoaa.data.api.SchoolApiService
import com.suseoaa.projectoaa.data.model.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * 教学执行计划仓库 - 处理修读要求和课程信息查询
 */
class TeachingPlanRepository(
    private val api: SchoolApiService,
    private val json: Json,
    private val authRepository: SchoolAuthRepository
) {

    /**
     * 获取学院列表
     * 通过获取所有专业列表，然后提取唯一的学院信息
     */
    suspend fun getCollegeList(): Result<List<CollegeOption>> = withContext(Dispatchers.IO) {
        try {
            // 不带学院ID参数，获取所有专业（包含学院信息）
            val response = api.getAllMajorList()
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val allMajors = json.decodeFromString<List<MajorApiResponse>>(bodyText)
                // 从专业列表中提取唯一的学院
                val colleges = allMajors
                    .filter { it.collegeId.isNotEmpty() && it.collegeName.isNotEmpty() }
                    .distinctBy { it.collegeId }
                    .map { CollegeOption(code = it.collegeId, name = it.collegeName) }
                    .sortedBy { it.name }
                Result.success(colleges)
            } else {
                Result.failure(Exception("获取学院列表失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取专业列表
     */
    suspend fun getMajorList(collegeId: String): Result<List<MajorOption>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllMajorList(collegeId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val allMajors = json.decodeFromString<List<MajorApiResponse>>(bodyText)
                // 转换为 MajorOption
                val majors = allMajors
                    .filter { it.majorId.isNotEmpty() }
                    .map { MajorOption(code = it.majorId, name = it.majorName) }
                    .sortedBy { it.name }
                Result.success(majors)
            } else {
                Result.failure(Exception("获取专业列表失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取培养计划信息
     */
    suspend fun getTeachingPlanInfo(
        collegeId: String,
        gradeId: String,
        majorId: String
    ): Result<TeachingPlanInfo?> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProfessionInfo(collegeId, gradeId, majorId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val planResponse = json.decodeFromString<TeachingPlanListResponse>(bodyText)
                val planInfo = planResponse.items.firstOrNull()
                Result.success(planInfo)
            } else {
                Result.failure(Exception("获取培养计划失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取修读要求节点下的课程
     */
    suspend fun getStudyRequirementCourses(
        requirementNodeId: String
    ): Result<List<StudyRequirementCourse>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStudyRequirementCourses(requirementNodeId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = json.decodeFromString<List<StudyRequirementCourse>>(bodyText)
                Result.success(courses)
            } else {
                Result.failure(Exception("获取修读要求课程失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取课程信息列表
     */
    suspend fun getCourseInfoList(
        planId: String,
        suggestedYear: String = "",
        suggestedSemester: String = "",
        courseCode: String = "",
        studyType: String = "",
        showCount: Int = 1000
    ): Result<CourseInfoListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTeachingPlan(
                planId = planId,
                suggestedYear = suggestedYear,
                suggestedSemester = suggestedSemester,
                courseCode = courseCode,
                studyType = studyType,
                showCount = showCount
            )
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courseResponse = json.decodeFromString<CourseInfoListResponse>(bodyText)
                Result.success(courseResponse)
            } else {
                Result.failure(Exception("获取课程信息失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 自动登录后获取课程信息
     */
    suspend fun getCourseInfoListWithAuth(
        studentId: String,
        password: String,
        planId: String,
        suggestedYear: String = "",
        suggestedSemester: String = "",
        courseCode: String = "",
        studyType: String = ""
    ): Result<CourseInfoListResponse> = withContext(Dispatchers.IO) {
        try {
            // 先尝试获取
            var result = getCourseInfoList(planId, suggestedYear, suggestedSemester, courseCode, studyType)
            
            // 如果失败，尝试登录后重试
            if (result.isFailure) {
                authRepository.login(studentId, password)
                result = getCourseInfoList(planId, suggestedYear, suggestedSemester, courseCode, studyType)
            }
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取学生当前专业的培养计划ID
     */
    suspend fun getStudentPlanId(
        studentId: String,
        password: String,
        collegeId: String,
        gradeId: String,
        majorId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 确保已登录
            authRepository.login(studentId, password)
            
            // 获取培养计划
            val planResult = getTeachingPlanInfo(collegeId, gradeId, majorId)
            if (planResult.isSuccess) {
                val plan = planResult.getOrNull()
                if (plan != null) {
                    Result.success(plan.planId)
                } else {
                    Result.failure(Exception("未找到培养计划"))
                }
            } else {
                Result.failure(planResult.exceptionOrNull() ?: Exception("获取培养计划失败"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 生成年级列表（从2004年到当前年份+1）
     */
    fun generateGradeList(): List<String> {
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .year
        return (currentYear + 1 downTo 2004).map { it.toString() }
    }

    /**
     * 将课程按类型分组
     */
    fun groupCoursesByType(courses: List<CourseInfoItem>): Map<String, List<CourseInfoItem>> {
        return courses.groupBy { it.courseType.ifEmpty { "其他" } }
    }

    /**
     * 按学期分组课程
     */
    fun groupCoursesBySemester(courses: List<CourseInfoItem>): Map<String, List<CourseInfoItem>> {
        return courses.groupBy { 
            "${it.suggestedYear} ${SemesterConstants.getSemesterName(it.suggestedSemester)}"
        }
    }

    /**
     * 计算总学分
     */
    fun calculateTotalCredits(courses: List<CourseInfoItem>): Double {
        return courses.sumOf { 
            it.credits.toDoubleOrNull() ?: 0.0 
        }
    }
}
