package com.suseoaa.projectoaa.core.network.model.changePassword

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
{
    "oldpassword": "hDq90ndSVTsoEi1",
    "password": "KGqYzstC5ytyEeY"
}
*/
@Keep
@Serializable
data class ChangePasswordRequest(
    @SerialName("oldpassword")
    val oldpassword: String,
    @SerialName("password")
    val password: String
)