package com.suseoaa.projectoaa.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RSAKey(
    @SerialName("modulus") val modulus: String,
    @SerialName("exponent") val exponent: String
)

@Serializable
data class CourseResponseJson(
    @SerialName("kbList") val kbList: List<CourseItem>? = null,
    @SerialName("xsxx") val xsxx: StudentInfo? = null
)

/**
 * 学生信息
 */
@Serializable
data class StudentInfo(
    @SerialName("BJMC") val className: String? = null,
    @SerialName("NJDM_ID") val njdmId: String? = null,
    @SerialName("XH") val studentNo: String? = null,
    @SerialName("XM") val name: String? = null,
    @SerialName("XNM") val xnm: String? = null,
    @SerialName("XQM") val xqm: String? = null,
    @SerialName("ZYH_ID") val zyhId: String? = null,
    @SerialName("ZYMC") val major: String? = null
)

@Serializable
data class CourseItem(
    @SerialName("kch_id") val kchId: String? = null,
    @SerialName("kcmc") val kcmc: String? = null,
    @SerialName("xnm") val xnm: String? = null,
    @SerialName("xqm") val xqm: String? = null,
    @SerialName("xqj") val xqj: String? = null,
    @SerialName("jcs") val jcs: String? = null,
    @SerialName("zcd") val zcd: String? = null,
    @SerialName("cdmc") val cdmc: String? = null,
    @SerialName("xm") val xm: String? = null,
    @SerialName("kcxzmc") val kcxzmc: String? = null,
    @SerialName("kclbmc") val kclbmc: String? = null,
    @SerialName("khfsmc") val khfsmc: String? = null,
    @SerialName("xf") val xf: String? = null,
    @SerialName("xqmc") val xqmc: String? = null,
    @SerialName("kkxy") val kkxy: String? = null
)
