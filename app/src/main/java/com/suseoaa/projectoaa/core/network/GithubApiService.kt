package com.suseoaa.projectoaa.core.network


import com.suseoaa.projectoaa.core.network.model.update.GithubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApiService {
    // 获取最新 Release
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRelease>
}