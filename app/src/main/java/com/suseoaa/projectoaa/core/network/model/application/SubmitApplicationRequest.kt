package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
    "name":"李华",
    "reason":"试试就逝世",
    "choice1":"组织宣传部",
    "choice2":"项目实践部",
    "experience":"无",
    "phone":"18780732003",
    "gender":"男",
    "major":"软件工程",
    "class":"245班",
    "birthday":"2006-02-03",
    "qq":"2824920336",
    "politic_stance":"团员",
    "adjustiment":0
}
*/
@Keep
@Serializable
data class SubmitApplicationRequest(
    @SerialName("adjustiment")
    val adjustiment: Int,
    @SerialName("birthday")
    val birthday: String,
    @SerialName("choice1")
    val choice1: String,
    @SerialName("choice2")
    val choice2: String,
    @SerialName("class")
    val classX: String,
    @SerialName("experience")
    val experience: String,
    @SerialName("gender")
    val gender: String,
    @SerialName("major")
    val major: String,
    @SerialName("name")
    val name: String,
    @SerialName("phone")
    val phone: String,
    @SerialName("politic_stance")
    val politicStance: String,
    @SerialName("qq")
    val qq: String,
    @SerialName("reason")
    val reason: String
)