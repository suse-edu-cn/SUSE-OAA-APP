package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GetExamInfoViewModel @Inject constructor(
    private val repository: SchoolInfoRepository,
    private val courseDao: CourseDao,
    tokenManager: TokenManager
) : ViewModel() {

    // 1. 获取当前账户流 (用于鉴权)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(courseDao.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 2. 考试列表流 (监听数据库)
    // 注意：变量名从 dataList 改为了 examList，UI 必须同步修改
    @OptIn(ExperimentalCoroutinesApi::class)
    val examList: StateFlow<List<ExamUiState>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            repository.observeExams(studentId)
        }
        .map { entities ->
            entities.map { entity ->
                ExamUiState(
                    courseName = entity.courseName,
                    time = entity.time,
                    location = entity.location
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. 刷新状态
    var isRefreshing by mutableStateOf(false)
        private set

    // 4. 手动刷新方法 (替代原来的 fetchData)
    fun refreshData() {
        // [关键修复] 直接使用 currentAccount.value 获取当前账户
        val account = currentAccount.value ?: return

        viewModelScope.launch {
            if (isRefreshing) return@launch
            isRefreshing = true
            repository.refreshAcademicExamInfo(account)
            isRefreshing = false
        }
    }
}