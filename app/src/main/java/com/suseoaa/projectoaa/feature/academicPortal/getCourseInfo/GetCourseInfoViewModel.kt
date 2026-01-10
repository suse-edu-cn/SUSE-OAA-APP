package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.ui.BaseInfoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GetCourseInfoViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager,
) : BaseInfoViewModel<List<String>>(tokenManager, courseDao) {
    override suspend fun executeRequest(account: CourseAccountEntity): Result<List<String>> {
        // 这里调用具体的 AreaOne 接口
        return schoolRepository.getAcademicCourseInfo(account)
    }
}