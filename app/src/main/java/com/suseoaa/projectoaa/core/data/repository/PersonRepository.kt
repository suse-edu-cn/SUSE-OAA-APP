package com.suseoaa.projectoaa.core.data.repository

import com.suseoaa.projectoaa.core.network.model.person.Data
import com.suseoaa.projectoaa.core.network.person.PersonService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val api: PersonService
) {
    suspend fun getPersonInfo(): Result<Data> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPersonInfo()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 200) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "获取用户信息失败"))
                }
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}