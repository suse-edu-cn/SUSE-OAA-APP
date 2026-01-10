package com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
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

    // 1. 定义数据流：List<String>
    private val _courseInfoList = MutableStateFlow<List<String>>(emptyList())
    val courseInfoList: StateFlow<List<String>> = _courseInfoList.asStateFlow()

    // 定义一个状态来表示加载中（可选，优化体验）
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 2. 修改函数：没有返回值，只是触发请求并更新状态
    fun fetchAcademicCourseInfo() {
        viewModelScope.launch {
            _isLoading.value = true

            // [优化] 等待账号加载，而不是直接 return
            // filterNotNull().first() 会挂起直到拿到非空的账号
            val account = currentAccount.filterNotNull().first()

            // 调用 Repository (它现在会自动处理登录了)
            val result = schoolRepository.getAcademicCourseInfo(account)

            result.onSuccess { list ->
                _courseInfoList.value = list
            }.onFailure { e ->
                _courseInfoList.value = listOf("获取失败: ${e.message}")
            }
            _isLoading.value = false
        }
    }
}