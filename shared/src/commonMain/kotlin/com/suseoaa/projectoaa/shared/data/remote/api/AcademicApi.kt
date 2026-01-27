package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamResponse
import com.suseoaa.projectoaa.shared.domain.model.grade.StudentGradeResponse
import com.suseoaa.projectoaa.shared.domain.model.message.AcademicMessageResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * 教务相关 API (成绩、考试等)
 */
class AcademicApi(private val client: HttpClient) {
    
    private val baseUrl = ApiConfig.BASE_URL

    /**
     * 获取学生成绩
     */
    suspend fun getStudentGrades(
        xnm: String? = null,
        xqm: String? = null
    ): StudentGradeResponse {
        return client.get("$baseUrl/api/academic/grades") {
            xnm?.let { parameter("xnm", it) }
            xqm?.let { parameter("xqm", it) }
        }.body()
    }

    /**
     * 获取考试信息
     */
    suspend fun getExamInfo(): ExamResponse {
        return client.get("$baseUrl/api/academic/exams").body()
    }

    /**
     * 获取教务消息 (调课通知等)
     */
    suspend fun getAcademicMessages(): AcademicMessageResponse {
        return client.get("$baseUrl/api/academic/messages").body()
    }
}
