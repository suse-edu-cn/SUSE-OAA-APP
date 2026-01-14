package com.suseoaa.projectoaa.core.network.person

import com.suseoaa.projectoaa.core.network.model.person.PersonResponse
import retrofit2.Response
import retrofit2.http.GET

interface PersonService {
    @GET("/user/Info")
    suspend fun getPersonInfo(): Response<PersonResponse>
}