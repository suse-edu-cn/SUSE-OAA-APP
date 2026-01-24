package com.suseoaa.projectoaa.core.network.model.application


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataX(
    @SerialName("adjustment")
    val adjustment: Int,
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
    val reason: String,
    @SerialName("student_id")
    val studentId: String
)