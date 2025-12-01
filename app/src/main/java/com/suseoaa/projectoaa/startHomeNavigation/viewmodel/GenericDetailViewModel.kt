package com.suseoaa.projectoaa.navigation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.navigation.repository.detail.DetailRepository // 依赖接口
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 详情页的UI状态
 */
data class DetailScreenState(
    val isLoading: Boolean = true, // [修复] 1. 在这里添加 isLoading 字段
    val title: String = "加载中...",
    val blocks: List<DetailBlock> = emptyList()
)

/**
 * 代表一个“标准信息块”的数据
 */
data class DetailBlock(
    val title: String,
    val content: String
)

@HiltViewModel
class GenericDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DetailRepository, // Hilt 会注入 FakeDetailRepository
    private val sessionManager: SessionManager
) : ViewModel() {

    var uiState by mutableStateOf(DetailScreenState())
        private set

    init {
        val taskId = savedStateHandle.get<String>("taskId") ?: "0"
        loadData(taskId)
    }

    private fun loadData(taskId: String) {
        viewModelScope.launch {
            // 2. [修复] 确保在开始时设置 isLoading = true
            uiState = DetailScreenState(isLoading = true, title = "加载中...")

            val token = sessionManager.jwtToken
            if (token.isNullOrBlank()) {
                uiState = DetailScreenState(
                    isLoading = false,
                    title = "错误",
                    blocks = listOf(DetailBlock("认证失败", "您尚未登录或登录已过期。"))
                )
                return@launch
            }

            // 3. ViewModel 调用接口 (Hilt 提供了 Fake 实现)
            val result = repository.getTaskDetails(token, taskId)

            // 4. 处理成功/失败，并设置 isLoading = false
            result.onSuccess { (title, blocks) ->
                uiState = DetailScreenState(
                    isLoading = false,
                    title = title,
                    blocks = blocks
                )
            }.onFailure { error ->
                uiState = DetailScreenState(
                    isLoading = false,
                    title = "加载失败",
                    blocks = listOf(DetailBlock("错误详情", error.message ?: "未知错误"))
                )
            }
        }
    }
}