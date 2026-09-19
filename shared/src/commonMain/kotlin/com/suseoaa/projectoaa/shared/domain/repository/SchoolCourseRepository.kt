package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import io.ktor.client.statement.*
import com.suseoaa.projectoaa.shared.domain.model.course.SemesterCalendarInfo

/**
 * SchoolCourseRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface SchoolCourseRepository {

    suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson>

    suspend fun fetchSemesterStart(): SemesterCalendarInfo?
}
