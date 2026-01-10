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

    // === 1. 通用的状态管理 ===
    private val _dataList = MutableStateFlow<T?>(null)
    val dataList: StateFlow<T?> = _dataList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // 通用的账号获取逻辑
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAccount: StateFlow<CourseAccountEntity?> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            flow { emit(courseDao.getAccountById(studentId)) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // === 2. 抽象方法：留给子类去实现的“填空题” ===
    // 子类必须告诉父类：具体的网络请求该怎么发？
    protected abstract suspend fun executeRequest(account: CourseAccountEntity): Result<T>

    // === 3. 通用的获取逻辑 ===
    // 这个方法是父类写好的，子类直接继承就有，不用重写
    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 等待账号
            val account = currentAccount.filterNotNull().first()

            // 调用抽象方法 (执行子类的逻辑)
            val result = executeRequest(account)

            result.onSuccess { data ->
                _dataList.value = data
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "未知错误"
                // 可以考虑把错误塞进一个专门的 error list 或者用 toast channel
            }

            _isLoading.value = false
        }
    }
}