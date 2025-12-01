package com.suseoaa.projectoaa.student.util

import com.suseoaa.projectoaa.student.model.StudentApplicationData
import com.suseoaa.projectoaa.student.model.FormFieldErrors
import javax.inject.Inject // 2. 导入

class FormValidator @Inject constructor() {

    fun validateInput(data: StudentApplicationData): FormFieldErrors {
        val nameError = if (data.name.isBlank()) "需填写" else null
        val collegeError = if (data.college.isBlank()) "需填写" else null
        val majorClassError = if (data.majorClass.isBlank()) "需填写" else null
        val politicalError = if (data.politicalStatus.isBlank()) "需填写" else null
        val birthDateError = if (data.birthDate.isBlank()) "需选择" else null

        val firstChoiceError = if (data.firstChoice.isBlank()) "需选择" else null

        val phoneRegex = Regex("^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\\d{8}$")
        val phoneError = when {
            data.phoneNumber.isBlank() -> "需填写"
            !data.phoneNumber.matches(phoneRegex) -> "格式错误"
            else -> null
        }

        val qqError = when {
            data.qq.isBlank() -> "需填写"
            !data.qq.matches(Regex("^\\d{5,13}$")) -> "格式错误"
            else -> null
        }

        val resumeError = if (data.resume.length < 10) "至少10个字" else null
        val reasonError = if (data.reason.length < 10) "至少10个字" else null

        // 互斥校验
        val secondChoiceConflictError =
            if (data.firstChoice.isNotEmpty() && data.secondChoice.isNotEmpty() && data.secondChoice != "无" && data.firstChoice == data.secondChoice) {
                "不能与第一志愿相同"
            } else {
                null
            }
        return FormFieldErrors(
            name = nameError,
            college = collegeError,
            majorClass = majorClassError,
            politicalStatus = politicalError,
            birthDate = birthDateError,
            phoneNumber = phoneError,
            qq = qqError,
            firstChoice = firstChoiceError,
            resume = resumeError,
            reason = reasonError,
            secondChoice = secondChoiceConflictError
        )
    }

}