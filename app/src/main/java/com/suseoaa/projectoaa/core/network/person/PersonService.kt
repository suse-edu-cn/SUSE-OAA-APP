package com.suseoaa.projectoaa.core.network.person

import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordResponse
import com.suseoaa.projectoaa.core.network.model.person.PersonResponse
import com.suseoaa.projectoaa.core.network.model.person.UpdatePersonResponse
import com.suseoaa.projectoaa.core.network.model.person.UpdateUserRequest
import com.suseoaa.projectoaa.core.network.model.person.UploadAvatarResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PersonService {
    // 1. 获取用户信息 -> 返回 PersonResponse (含 Data 对象)
    @GET("/user/Info")
    suspend fun getPersonInfo(): Response<PersonResponse>

    // 2. 修改密码 -> 返回 ChangePasswordResponse
    @POST("/user/updatePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ChangePasswordResponse>

    // 3. 修改用户信息 -> 返回 UpdatePersonResponse (处理 data: null)
    @POST("/user/update")
    suspend fun updateUserInfo(@Body request: UpdateUserRequest): Response<UpdatePersonResponse>

    // 4. 上传头像 -> 返回 UploadAvatarResponse (处理 data: String URL)
    @Multipart
    @POST("/user/uploadimg")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<UploadAvatarResponse>
}