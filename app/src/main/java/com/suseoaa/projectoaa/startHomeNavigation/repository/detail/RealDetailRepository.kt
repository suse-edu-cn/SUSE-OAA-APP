package com.suseoaa.projectoaa.navigation.repository.detail

import com.suseoaa.projectoaa.common.network.DetailApiService
import com.suseoaa.projectoaa.navigation.viewmodel.DetailBlock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 详情仓库的 "Real" 实现，用于对接真实后端。
 */
@Singleton
class RealDetailRepository @Inject constructor(
    private val api: DetailApiService
) : DetailRepository {

    override suspend fun getTaskDetails(token: String, taskId: String): Result<Pair<String, List<DetailBlock>>> {
        return try {
            // 1. 调用真实 API
            val response = api.getTaskDetails("Bearer $token", taskId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // 2. 映射 DTO -> UI Model
                val blocks = body.blocks.map { dto ->
                    DetailBlock(title = dto.title, content = dto.content)
                }

                Result.success(Pair(body.title, blocks))

            } else {
                Result.failure(Exception("加载失败: (Code ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误: ${e.message}"))
        }
    }
}