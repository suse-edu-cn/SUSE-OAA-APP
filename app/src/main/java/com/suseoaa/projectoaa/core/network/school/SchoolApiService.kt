package com.suseoaa.projectoaa.core.network.school

import com.suseoaa.projectoaa.core.network.model.academic.exam.ExamResponse
import com.suseoaa.projectoaa.core.network.model.academic.studentGrade.StudentGradeResponse
import com.suseoaa.projectoaa.core.network.model.course.RSAKey
import com.suseoaa.projectoaa.core.network.model.gpa.MajorItem
import com.suseoaa.projectoaa.core.network.model.gpa.ProfessionInfoResponse
import com.suseoaa.projectoaa.core.network.model.gpa.TeachingPlanResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SchoolApiService {
    @GET("/xtgl/login_slogin.html")
    suspend fun getCSRFToken(): ResponseBody

    @GET("/xtgl/login_getPublicKey.html")
    suspend fun getRSAKey(): RSAKey

    @FormUrlEncoded
    @POST("/xtgl/login_slogin.html")
    suspend fun login(
        @Query("time") timestamp: String,
        @Field("yhm") username: String,
        @Field("mm") encryptedPassword: String,
        @Field("csrftoken") csrfToken: String
    ): Response<ResponseBody>

    @GET
    suspend fun visitUrl(@Url url: String): ResponseBody

    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/kbcx/xskbcx_cxXsKb.html")
    suspend fun querySchedule(
        @Query("gnmkdm") gnmkdm: String = "N2151",
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("kzlx") kzlx: String = "ck"
    ): Response<ResponseBody>

    // 获取校历
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/xtgl/index_cxAreaSix.html")
    suspend fun getCalendar(
        @Query("localeKey") localeKey: String = "zh_CN",
        @Query("gnmkdm") gnmkdm: String = "index"
    ): Response<ResponseBody>


    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/cjcx/cjcx_cxDgXscj.html?doType=query&gnmkdm=N305005")
    suspend fun getStudentGrade(
        @Field("xnm") year: String = "",
        @Field("xqm") semester: String = "",
        @Field("queryModel.showCount") showCount: Int = 100,
        @Field("queryModel.currentPage") currentPage: Int = 1,
        @Field("queryModel.sortName") sortName: String = "",
        @Field("queryModel.sortOrder") sortOrder: String = "asc",
        @Field("_search") search: String = "false",
        @Field("nd") nd: Long = System.currentTimeMillis(),
        @Field("time") time: Int = 0
    ): Response<ResponseBody>


    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105")
    suspend fun getExamList(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("queryModel.showCount") showCount: Int = 100,
        @Field("queryModel.currentPage") currentPage: Int = 1,
        @Field("queryModel.sortName") sortName: String = "",
        @Field("queryModel.sortOrder") sortOrder: String = "asc"
    ): Response<ExamResponse>

    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/xtgl/index_cxAreaThree.html")
    suspend fun getAcademicMessageInfo(
        @Query("localeKey") localeKey: String = "zh_CN",
        @Query("gnmkdm") gnmkdm: String = "index"
    ): Response<ResponseBody>

    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/xtgl/index_cxAreaOne.html")
    suspend fun getAcademicCourseInfo(
        @Query("localeKey") localeKey: String = "zh_CN",
        @Query("gnmkdm") gnmkdm: String = "index"
    ): Response<ResponseBody>


    // 1. 获取专业代码列表
    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/xtgl/comm_cxZydmList.html")
    suspend fun getMajorList(
        @Query("jg_id") jgId: String,
        @Query("gnmkdm") gnmkdm: String = "N153540",
        @Field("dn") dn: String = "ai"
    ): Response<List<MajorItem>>

    // 2. 获取专业培养计划信息
    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/jxzxjhgl/jxzxjhck_cxJxzxjhckIndex.html?doType=query&gnmkdm=N153540")
    suspend fun getProfessionInfo(
        @Field("jg_id") jgId: String,
        @Field("njdm_id") grade: String,
        @Field("zyh_id") majorId: String,
        @Field("dlbs") dlbs: String = "",
        @Field("currentPage_cx") currentPageCx: String = "",
        @Field("_search") search: String = "false",
        @Field("nd") nd: Long = System.currentTimeMillis(),
        @Field("queryModel.showCount") showCount: Int = 100,
        @Field("queryModel.currentPage") currentPage: Int = 1,
        @Field("queryModel.sortName") sortName: String = " ",
        @Field("queryModel.sortOrder") sortOrder: String = "asc",
        @Field("time") time: Int = 0
    ): Response<ProfessionInfoResponse>

    // 3. 获取课程列表
    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("/jxzxjhgl/jxzxjhkcxx_cxJxzxjhkcxxIndex.html?doType=query&gnmkdm=N153540")
    suspend fun getTeachingPlan(
        @Field("jxzxjhxx_id") planId: String,
        @Field("jyxdxnm") jyxdxnm: String = "",
        @Field("jyxdxqm") jyxdxqm: String = "",
        @Field("yxxdxnm") yxxdxnm: String = "",
        @Field("yxxdxqm") yxxdxqm: String = "",
        @Field("shzt") shzt: String = "",
        @Field("kch") kch: String = "",
        @Field("xdlx") xdlx: String = "",
        @Field("_search") search: String = "false",
        @Field("nd") nd: Long = System.currentTimeMillis(),
        @Field("queryModel.showCount") showCount: Int = 1000,
        @Field("queryModel.currentPage") currentPage: Int = 1,
        @Field("queryModel.sortName") sortName: String = "jyxdxnm,jyxdxqm,kch ",
        @Field("queryModel.sortOrder") sortOrder: String = "asc",
        @Field("time") time: Int = 0
    ): Response<TeachingPlanResponse>
}