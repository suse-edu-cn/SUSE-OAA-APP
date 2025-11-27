package com.suseoaa.projectoaa.login.model

data class UserInfoData(
    val studentid: Long,
    val name: String,
    val username: String,
    val avatar: String?,
    val department: String,
    val role: String
)

data class UserInfoResponse(
    val code: Int,
    val message: String,
    val data: UserInfoData?
)