package com.suseoaa.projectoaa.competition.repository

import com.suseoaa.projectoaa.competition.model.*
import com.suseoaa.projectoaa.competition.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 比赛列表
     */
    suspend fun getMatchList(): List<MatchItem> {
        val response = apiService.getMatchList()
        if (response.code == 200 && response.data != null) {
            return response.data
        } else {
            throw Exception(response.message)
        }
    }

    /**
     * 比赛详情
     */
    suspend fun getMatchDetail(id: Int): MatchDetail {
        val response = apiService.getMatchDetail(id)
        if (response.code == 200 && response.data != null) {
            return response.data
        } else {
            throw Exception(response.message)
        }
    }

    /**
     * 创建比赛
     */
    suspend fun createMatch(token: String, request: CreateMatchRequest): Boolean {
        val response = apiService.createMatch(token, request)
        if (response.code == 0 || response.code == 200) {
            return true
        } else {
            throw Exception(response.message)
        }
    }
}