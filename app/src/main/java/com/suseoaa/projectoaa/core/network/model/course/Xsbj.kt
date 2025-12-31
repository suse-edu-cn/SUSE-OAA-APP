package com.suseoaa.projectoaa.core.network.model.course


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Xsbj(
    @SerialName("xslxbj") val xslxbj: String? = null,
    @SerialName("xsmc") val xsmc: String? = null,
    @SerialName("xsdm") val xsdm: String? = null
)
