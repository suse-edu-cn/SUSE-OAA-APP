package com.suseoaa.projectoaa.core.network.application

import com.suseoaa.projectoaa.core.network.model.application.ChangeSubmitTimeRequest
import com.suseoaa.projectoaa.core.network.model.application.ChangeSubmitTimeResponse
import com.suseoaa.projectoaa.core.network.model.application.Data
import com.suseoaa.projectoaa.core.network.model.application.GetApplicationBaseResponse
import com.suseoaa.projectoaa.core.network.model.application.SubmitApplicationRequest
import com.suseoaa.projectoaa.core.network.model.application.SubmitApplicationResponse
import com.suseoaa.projectoaa.core.network.model.application.UpdateApplicationResponse
import com.suseoaa.projectoaa.core.network.model.application.UploadApplicationAvatarResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApplicationService {
    //    获取申请列表
    @GET("/application/get")
    suspend fun getApplication(): Response<GetApplicationBaseResponse<List<Data>>>

    //    提交申请列表
    @POST("/application/create")
    suspend fun submitApplication(@Body request: SubmitApplicationRequest): Response<SubmitApplicationResponse>

    //    修改申请表
    @POST("/application/update")
    suspend fun updateApplication(@Body request: SubmitApplicationRequest): Response<UpdateApplicationResponse>

    //    上传申请表头像
    @Multipart
    @POST("/application/uploadimg")
    suspend fun uploadApplicationAvatar(@Part file: MultipartBody.Part): Response<UploadApplicationAvatarResponse>

    //    修改提交时间
    @POST("/application/updatetime")
    suspend fun changeSubmitTime(@Body request: ChangeSubmitTimeRequest): Response<ChangeSubmitTimeResponse>
}