package com.suseoaa.projectoaa.core.network.model.course

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class RSAKey(
    val modulus: String,
    val exponent: String
)
