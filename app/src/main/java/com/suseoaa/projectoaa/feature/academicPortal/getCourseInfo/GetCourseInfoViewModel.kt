package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
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
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            flow {
                emit(courseDao.getAccountById(studentId))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun fetchAcademicCourseInfo() {
        viewModelScope.launch {
            print("进入account")
            val account = currentAccount.value ?: return@launch
            print("进入account")
            val result = schoolRepository.getAcademicCourseInfo(account)
        }
    }
}