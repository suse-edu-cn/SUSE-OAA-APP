package com.suseoaa.projectoaa.core.network.school

import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import com.suseoaa.projectoaa.core.network.model.course.RSAKey
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SchoolApiService {

    // === 1. 登录准备 ===
    @GET("/xtgl/login_slogin.html")
    suspend fun getCSRFToken(): ResponseBody

    @GET("/xtgl/login_getPublicKey.html")
    suspend fun getRSAKey(): RSAKey

    // === 2. 执行登录 ===
    @FormUrlEncoded
    @POST("/xtgl/login_slogin.html")
    suspend fun login(
        @Query("time") timestamp: String,
        @Field("yhm") username: String,
        @Field("mm") encryptedPassword: String,
        @Field("csrftoken") csrfToken: String
    ): Response<ResponseBody>

    // === 3. 课表查询 ===
    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest") // 必须加！
    @POST("/kbcx/xskbcx_cxXsKb.html")
    suspend fun querySchedule(
        @Query("gnmkdm") gnmkdm: String = "N2151",
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("kzlx") kzlx: String = "ck"
    ): Response<ResponseBody>

    // === 4. 获取校历 (起始周) ===
    @Headers("X-Requested-With: XMLHttpRequest") // 必须加！
    @POST("/xtgl/index_cxAreaSix.html")
    suspend fun getCalendar(
        @Query("localeKey") localeKey: String = "zh_CN",
        @Query("gnmkdm") gnmkdm: String = "index"
    ): Response<ResponseBody>

    // === 辅助：重定向访问 ===
    @GET
    suspend fun visitUrl(@Url url: String): ResponseBody

    //成绩查询
    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/cjcx/cjcx_cxDgXscj.html?doType=query&gnmkdm=N305005")
    suspend fun getStudentGrade(
        // 学年
        @Field("xnm") year: String,
        // 学期
        @Field("xqm") semester: String,
        // 一页显示多少条，直接设大点，不用分页了
        @Field("queryModel.showCount") showCount: Int = 100,
        @Field("queryModel.currentPage") currentPage: Int = 1,
        @Field("queryModel.sortName") sortName: String = "",
        @Field("queryModel.sortOrder") sortOrder: String = "asc",
        @Field("_search") search: String = "false",
        // 当前时间戳
        @Field("nd") nd: Long = System.currentTimeMillis(),
        @Field("time") time: Int = 0
    ): Response<ResponseBody>
}