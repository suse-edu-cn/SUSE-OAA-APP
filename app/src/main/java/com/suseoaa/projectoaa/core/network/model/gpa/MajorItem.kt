package com.suseoaa.projectoaa.core.network.model.gpa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MajorItem(
    @SerialName("zyh_id") val majorId: String? = null, // e.g. "1013"
    @SerialName("zymc") val majorName: String? = null, // e.g. "网络工程(1013)"
    @SerialName("jg_id") val collegeId: String? = null, // e.g. "10"
    @SerialName("zyh") val zyh: String? = null
)
