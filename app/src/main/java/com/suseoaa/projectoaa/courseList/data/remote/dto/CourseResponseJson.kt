package com.suseoaa.projectoaa.courseList.data.remote.dto


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CourseResponseJson(
    @Json(name = "djdzList")
    val djdzList: List<Any?>?,
    @Json(name = "jfckbkg")
    val jfckbkg: Boolean?,
    @Json(name = "jxhjkcList")
    val jxhjkcList: List<Any?>?,
    @Json(name = "kbList")
    val kbList: List<Kb?>?,
    @Json(name = "kblx")
    val kblx: Int?,
    @Json(name = "qsxqj")
    val qsxqj: String?,
    @Json(name = "rqazcList")
    val rqazcList: List<Any?>?,
    @Json(name = "sfxsd")
    val sfxsd: String?,
    @Json(name = "sjfwkg")
    val sjfwkg: Boolean?,
    @Json(name = "sjkList")
    val sjkList: List<Any?>?,
    @Json(name = "sxgykbbz")
    val sxgykbbz: String?,
    @Json(name = "xkkg")
    val xkkg: Boolean?,
    @Json(name = "xnxqsfkz")
    val xnxqsfkz: String?,
    @Json(name = "xqbzxxszList")
    val xqbzxxszList: List<Any?>?,
    @Json(name = "xqjmcMap")
    val xqjmcMap: Map<String, String>?,
    @Json(name = "xsbjList")
    val xsbjList: List<Xsbj?>?,
    @Json(name = "xskbsfxstkzt")
    val xskbsfxstkzt: String?,
    @Json(name = "xsxx")
    val xsxx: Xsxx?,
    @Json(name = "zckbsfxssj")
    val zckbsfxssj: String?
)