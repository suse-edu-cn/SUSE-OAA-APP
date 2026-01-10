package com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo

import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GetAcademicMessageInfoViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager
) : BaseInfoViewModel<List<String>>(tokenManager, courseDao) {
    override suspend fun executeRequest(account: CourseAccountEntity): Result<List<String>> {
        // 这里调用具体的 AreaOne 接口
        return schoolRepository.getAcademicMessageInfo(account)
    }
}