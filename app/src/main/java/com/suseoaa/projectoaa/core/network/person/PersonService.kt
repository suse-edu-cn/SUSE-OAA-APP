package com.suseoaa.projectoaa.core.network.person

import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordRequest
import com.suseoaa.projectoaa.core.network.model.changePassword.ChangePasswordResponse
import com.suseoaa.projectoaa.core.network.model.person.PersonResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PersonService {
    @GET("/user/Info")
    suspend fun getPersonInfo(): Response<PersonResponse>


    @POST("/user/updatePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ChangePasswordResponse>
}