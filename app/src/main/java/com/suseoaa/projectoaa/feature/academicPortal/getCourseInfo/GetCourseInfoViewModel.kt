package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import com.suseoaa.projectoaa.core.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GetCourseInfoViewModel @Inject constructor(
    private val repository: SchoolInfoRepository,
    courseDao: CourseDao,
    tokenManager: TokenManager
) : BaseInfoViewModel<List<String>>(tokenManager, courseDao) {

    // 自动刷新
    init {
        fetchData()
    }

    override suspend fun executeRequest(account: CourseAccountEntity): Result<List<String>> {
        return repository.getAcademicCourseInfo(account)
    }
}