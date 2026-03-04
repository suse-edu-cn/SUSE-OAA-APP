package com.suseoaa.projectoaa.presentation.recruitment

import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepository

sealed class Recruitment{

}

class RecruitmentViewModel(
    private val repository: RecruitmentRepository
) {
}