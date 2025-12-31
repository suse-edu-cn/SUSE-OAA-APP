package com.suseoaa.projectoaa.core.network.model.course


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Xsxx(
    @SerialName("BJMC")
    val bJMC: String? = null,
    @SerialName("JFZT")
    val jFZT: Int? = null,
    @SerialName("KCMS")
    val kCMS: Int? = null,
    @SerialName("KXKXXQ")
    val kXKXXQ: String? = null,
    @SerialName("NJDM_ID")
    val nJDMID: String? = null,
    @SerialName("XH")
    val xH: String? = null,
    @SerialName("XH_ID")
    val xHID: String? = null,
    @SerialName("XKKG")
    val xKKG: String? = null,
    @SerialName("XKKGXQ")
    val xKKGXQ: String? = null,
    @SerialName("XM")
    val xM: String? = null,
    @SerialName("XNM")
    val xNM: String? = null,
    @SerialName("XNMC")
    val xNMC: String? = null,
    @SerialName("XQM")
    val xQM: String? = null,
    @SerialName("XQMMC")
    val xQMMC: String? = null,
    @SerialName("ZYH_ID")
    val zYHID: String? = null,
    @SerialName("ZYMC")
    val zYMC: String? = null
)
