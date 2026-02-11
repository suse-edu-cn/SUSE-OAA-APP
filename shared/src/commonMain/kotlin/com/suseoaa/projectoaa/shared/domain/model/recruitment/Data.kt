package com.suseoaa.projectoaa.shared.domain.model.recruitment


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("adjustment")
    val adjustment: Int,
    @SerialName("avator")
    val avator: String,
    @SerialName("birthday")
    val birthday: String,
    @SerialName("choice1")
    val choice1: String,
    @SerialName("choice2")
    val choice2: String,
    @SerialName("class")
    val classX: String,
    @SerialName("created_at")
    val createdAt: String,
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
    @SerialName("role1")
    val role1: String,
    @SerialName("role2")
    val role2: String,
    @SerialName("status")
    val status: String,
    @SerialName("student_id")
    val studentId: String
)