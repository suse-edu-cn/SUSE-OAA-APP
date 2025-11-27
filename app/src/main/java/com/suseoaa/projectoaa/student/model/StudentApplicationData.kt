package com.suseoaa.projectoaa.student.model

import android.net.Uri

data class StudentApplicationData(
    val type: String = "招新",
    val name: String = "",
    val gender: String = "男",
    val college: String = "",
    val majorClass: String = "",
    val politicalStatus: String = "",
    val birthDate: String = "",
    val qq: String = "",
    val phoneNumber: String = "",
    val firstChoice: String = "",
    val secondChoice: String = "",
    val isObeyAdjustment: Boolean = true,
    val photoUri: Uri? = null,
    val resume: String = "",
    val reason: String = ""
)

data class FormFieldErrors(
    val name: String? = null,
    val college: String? = null,
    val majorClass: String? = null,
    val politicalStatus: String? = null,
    val birthDate: String? = null,
    val phoneNumber: String? = null,
    val qq: String? = null,
    val photoError: String? = null,
    val resume: String? = null,
    val reason: String? = null,
    val firstChoice: String? = null,
    val secondChoice: String? = null
) {
    fun hasErrors(): Boolean {
        // 确保所有字段都被检查
        return name != null || college != null || majorClass != null ||
                politicalStatus != null || birthDate != null || phoneNumber != null ||
                qq != null || firstChoice != null || photoError != null ||
                resume != null || reason != null || secondChoice != null
    }
}