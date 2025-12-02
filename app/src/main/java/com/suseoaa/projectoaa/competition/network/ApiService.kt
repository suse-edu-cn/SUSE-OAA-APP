package com.suseoaa.projectoaa.competition.network // 假设的包名

import com.suseoaa.projectoaa.competition.model.*

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/matches/list")
    suspend fun getMatchList(): ApiResponse<List<MatchItem>>

    @GET("api/match/detail")
    suspend fun getMatchDetail(@Query("id") id: Int): ApiResponse<MatchDetail>
}