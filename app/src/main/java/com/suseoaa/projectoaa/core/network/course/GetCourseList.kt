package com.suseoaa.projectoaa.core.network.course

import com.suseoaa.projectoaa.core.network.model.course.RSAKey
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import okhttp3.ResponseBody
import retrofit2.http.Headers


interface RsaKeyAPI {
    @GET("/xtgl/login_getPublicKey.html")
    suspend fun getrsaKey(): RSAKey
}

interface GetCSRFToken {
    @GET("/xtgl/login_slogin.html")
    suspend fun getCSRFToken(): ResponseBody
}

// 登录接口
interface LoginAPI {
    @FormUrlEncoded
    @POST("/xtgl/login_slogin.html")
    suspend fun login(
        @Query("time") timestamp: String,
        @Field("yhm") username: String,
        @Field("mm") encryptedPassword: String,
        @Field("csrftoken") csrfToken: String
    ): Response<ResponseBody>
}

// 课表查询接口
interface ScheduleAPI {
    @GET("/kbcx/xskbcx_cxXsKb.html")
    suspend fun getSchedulePage(@Query("gnmkdm") gnmkdm: String = "N2151"): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/kbcx/xskbcx_cxXsKb.html")
    suspend fun querySchedule(
        @Query("gnmkdm") gnmkdm: String = "N2151",
        @Field("xqm") semester: String, // 3 或 12
        @Field("xnm") year: String,     // 例如 2024
        @Field("kzlx") kzlx: String = "ck"
    ): Response<ResponseBody>
}

// 重定向处理接口
interface RedirectAPI {
    @GET
    suspend fun visitUrl(@Url url: String): ResponseBody
}

//获取校历
interface CalendarAPI {
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/xtgl/index_cxAreaSix.html")
    suspend fun getCalendar(
        @Query("localeKey") localeKey: String = "zh_CN",
        @Query("gnmkdm") gnmkdm: String = "index"
    ): Response<ResponseBody>
}