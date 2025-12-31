package com.suseoaa.projectoaa.core.network.model.course

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Keep
@Serializable
data class CourseResponseJson(
    @SerialName("djdzList")
    val djdzList: List<JsonElement>? = null,
    @SerialName("jfckbkg")
    val jfckbkg: Boolean? = null,
    @SerialName("jxhjkcList")
    val jxhjkcList: List<JsonElement>? = null,
    @SerialName("kbList")
    val kbList: List<Kb?>? = null,
    @SerialName("kblx")
    val kblx: Int? = null,
    @SerialName("qsxqj")
    val qsxqj: String? = null,
    @SerialName("rqazcList")
    val rqazcList: List<JsonElement>? = null,
    @SerialName("sfxsd")
    val sfxsd: String? = null,
    @SerialName("sjfwkg")
    val sjfwkg: Boolean? = null,
    @SerialName("sjkList")
    val sjkList: List<JsonElement>? = null,
    @SerialName("sxgykbbz")
    val sxgykbbz: String? = null,
    @SerialName("xkkg")
    val xkkg: Boolean? = null,
    @SerialName("xnxqsfkz")
    val xnxqsfkz: String? = null,
    @SerialName("xqbzxxszList")
    val xqbzxxszList: List<JsonElement>? = null,
    @SerialName("xqjmcMap")
    val xqjmcMap: Map<String, String>? = null,
    @SerialName("xsbjList")
    val xsbjList: List<Xsbj?>? = null,
    @SerialName("xskbsfxstkzt")
    val xskbsfxstkzt: String? = null,
    @SerialName("xsxx")
    val xsxx: Xsxx? = null,
    @SerialName("zckbsfxssj")
    val zckbsfxssj: String? = null
)
