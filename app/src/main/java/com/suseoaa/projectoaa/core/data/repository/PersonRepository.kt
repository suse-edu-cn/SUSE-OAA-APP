package com.suseoaa.projectoaa.core.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.core.network.model.person.Data
import com.suseoaa.projectoaa.core.network.model.person.UpdateUserRequest
import com.suseoaa.projectoaa.core.network.person.PersonService
import com.suseoaa.projectoaa.core.network.school.SchoolCookieJar
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val api: PersonService,
    private val tokenManager: TokenManager,
    private val cookieJar: SchoolCookieJar,
    @param:ApplicationContext private val context: Context
) {

    suspend fun logout() {
        tokenManager.clearToken()
        cookieJar.clear()
    }

    // 1. 获取用户信息
    suspend fun getPersonInfo(): Result<Data> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPersonInfo()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 200) {
                    val data = body.data
                    if (data != null) Result.success(data)
                    else Result.failure(Exception("用户信息为空"))
                } else {
                    Result.failure(Exception(body?.message ?: "获取失败"))
                }
            } else {
                // 读取错误详情
                val errorMsg = response.errorBody()?.string() ?: "请求失败: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. 修改密码
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = ChangePasswordRequest(oldPassword, newPassword)
                val response = api.changePassword(request)
                if (response.isSuccessful && response.body()?.code == 200) {
                    Result.success(response.body()?.message ?: "修改成功")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.body()?.message
                    ?: "修改失败: ${response.code()}"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 3. 修改用户信息
    suspend fun updateUserInfo(username: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateUserInfo(UpdateUserRequest(username, name))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        Result.success(body.message)
                    } else {
                        val msg = body?.message ?: "修改失败"
                        // 打印业务逻辑失败日志
                        Log.e("PersonRepo", "业务失败: $msg")
                        Result.failure(Exception(msg))
                    }
                } else {
                    // 解析 errorBody
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrBlank()) {
                        "服务器错误(${response.code()}): $errorBody"
                    } else {
                        "服务器错误(${response.code()})"
                    }

                    // 【在此处添加日志打印】
                    // 第一个参数是 TAG (标签)，方便在 Logcat 中筛选
                    // 第二个参数是具体的日志内容
                    Log.e("PersonRepo", "请求失败: $errorMessage")

                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // 捕获网络异常或其他崩溃
                e.printStackTrace()
                Log.e("PersonRepo", "异常捕获: ${e.message}", e)
                Result.failure(e)
            }
        }

    // 4. 上传头像
    suspend fun uploadAvatar(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var originalFile: File? = null
        try {
            originalFile =
                uriToFile(uri) ?: return@withContext Result.failure(Exception("读取文件失败"))

            val compressedFile = Compressor.compress(context, originalFile) {
                default(width = 800, height = 800, quality = 80)
                destination(
                    File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.jpg")
                )
            }

            val requestBody = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("Image", compressedFile.name, requestBody)

            val response = api.uploadAvatar(part)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 200) {
                    val url = body.data
                    if (!url.isNullOrBlank()) {
                        Result.success(url)
                    } else {
                        Result.success("上传成功")
                    }
                } else {
                    Result.failure(Exception(body?.message ?: "上传失败"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "上传失败: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            runCatching { originalFile?.delete() }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) {
            null
        }
    }
}