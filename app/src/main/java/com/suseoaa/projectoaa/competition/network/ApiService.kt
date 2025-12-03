package com.suseoaa.projectoaa.competition.network // 假设的包名

import com.suseoaa.projectoaa.competition.model.*
import retrofit2.http.Body

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("/match/getList")
    suspend fun getMatchList(): ApiResponse<List<MatchItem>>
    @GET("/match/getDetail")
    suspend fun getMatchDetail(@Query("id") id: Int): ApiResponse<MatchDetail>
    @POST("/contest/create")
    suspend fun createMatch(
        @Header("Authorization") token: String,
        @Body body: CreateMatchRequest
    ): ApiResponse<Unit?>
}