package com.suseoaa.projectoaa.courseList.data.remote.dto


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Xsxx(
    @Json(name = "BJMC")
    val bJMC: String?,
    @Json(name = "JFZT")
    val jFZT: Int?,
    @Json(name = "KCMS")
    val kCMS: Int?,
    @Json(name = "KXKXXQ")
    val kXKXXQ: String?,
    @Json(name = "NJDM_ID")
    val nJDMID: String?,
    @Json(name = "XH")
    val xH: String?,
    @Json(name = "XH_ID")
    val xHID: String?,
    @Json(name = "XKKG")
    val xKKG: String?,
    @Json(name = "XKKGXQ")
    val xKKGXQ: String?,
    @Json(name = "XM")
    val xM: String?,
    @Json(name = "XNM")
    val xNM: String?,
    @Json(name = "XNMC")
    val xNMC: String?,
    @Json(name = "XQM")
    val xQM: String?,
    @Json(name = "XQMMC")
    val xQMMC: String?,
    @Json(name = "ZYH_ID")
    val zYHID: String?,
    @Json(name = "ZYMC")
    val zYMC: String?
)