package com.suseoaa.projectoaa.student.network


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


interface ApiService {
    @POST("/application/create")
    suspend fun submitApplication(
        @Header("Authorization") token: String, // 需要 Header 鉴权
        @Body request: ApplicationRequest       // 发送 JSON 结构
    ): Response<ApplicationResponse>            // 接收响应

}
