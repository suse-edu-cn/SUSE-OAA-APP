package com.suseoaa.projectoaa.student.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.suseoaa.projectoaa.student.model.StudentApplicationData
import com.suseoaa.projectoaa.student.model.FormFieldErrors

object FormValidator {
    private const val MAX_IMAGE_SIZE_MB = 5
    private const val MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024

    // 返回 FormFieldErrors 对象
    fun validateInput(data: StudentApplicationData, context: Context): FormFieldErrors {
        val nameError = if (data.name.isBlank()) "需填写" else null
        val collegeError = if (data.college.isBlank()) "需填写" else null
        val majorClassError = if (data.majorClass.isBlank()) "需填写" else null
        val politicalError = if (data.politicalStatus.isBlank()) "需填写" else null
        val birthDateError = if (data.birthDate.isBlank()) "需选择" else null

        // 第一志愿错误，如果为空则报错
        val firstChoiceError = if (data.firstChoice.isBlank()) "需选择" else null

        // 电话号码校验
        val phoneRegex = Regex("^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\\d{8}$")

        val phoneError = when {
            data.phoneNumber.isBlank() -> "需填写"
            !data.phoneNumber.matches(phoneRegex) -> "格式错误"
            else -> null
        }

        // QQ校验
        val qqError = when {
            data.qq.isBlank() -> "需填写"
            !data.qq.matches(Regex("^\\d{5,13}$")) -> "格式错误"
            else -> null
        }

        val resumeError = if (data.resume.length < 10) "至少10个字" else null
        val reasonError = if (data.reason.length < 10) "至少10个字" else null

        // 图片校验
        var photoError: String? = null

        // ========================================================
        // 互斥校验 (如果两个志愿相同，且都不是空或“无”)
        // ========================================================
        val secondChoiceConflictError =
            if (data.firstChoice.isNotEmpty() && data.secondChoice.isNotEmpty() && data.secondChoice != "无" && data.firstChoice == data.secondChoice) {
                "不能与第一志愿相同"
            } else {
                null
            }

        // --- 返回最终的错误集合 ---
        return FormFieldErrors(
            name = nameError,
            college = collegeError,
            majorClass = majorClassError,
            politicalStatus = politicalError,
            birthDate = birthDateError,
            phoneNumber = phoneError,
            qq = qqError,
            firstChoice = firstChoiceError,
            photoError = photoError,
            resume = resumeError,
            reason = reasonError,
            secondChoice = secondChoiceConflictError // 互斥错误信息
        )
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
            } ?: 0L
        } catch (e: Exception) { 0L }
    }
}