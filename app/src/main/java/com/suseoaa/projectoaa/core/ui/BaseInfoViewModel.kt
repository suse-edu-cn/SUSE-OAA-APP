package com.suseoaa.projectoaa.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
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


abstract class BaseInfoViewModel<T>(
    private val tokenManager: TokenManager,
    private val courseDao: CourseDao
) : ViewModel() {

    private val _dataList = MutableStateFlow<T?>(null)
    val dataList: StateFlow<T?> = _dataList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            flow { emit(courseDao.getAccountById(studentId)) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    protected abstract suspend fun executeRequest(account: CourseAccountEntity): Result<T>

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 主动检查，而不是 filterNotNull().first() 死等
                val studentId = tokenManager.currentStudentId.first()
                if (studentId.isNullOrEmpty()) {
                    _errorMessage.value = "未登录"
                    return@launch
                }

                val account = courseDao.getAccountById(studentId)
                if (account == null) {
                    _errorMessage.value = "账号信息缺失"
                    return@launch
                }

                val result = executeRequest(account)

                result.onSuccess { data ->
                    _dataList.value = data
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "未知错误"
                }

            } catch (e: Exception) {
                _errorMessage.value = "发生错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}