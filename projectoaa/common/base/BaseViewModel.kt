package com.suseoaa.projectoaa.common.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 所有 ViewModel 的父类
 * 自动处理 loading 状态和 异常捕获
 */
abstract class BaseViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        protected set

    var errorMessage by mutableStateOf<String?>(null)

    /**
     * 一个安全的协程启动器，自动处理 try-catch 和 loading
     */
    protected fun launchDataLoad(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                block()
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "发生错误: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}