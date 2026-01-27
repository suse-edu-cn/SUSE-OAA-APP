package com.suseoaa.projectoaa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.ui.navigation.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 主 ViewModel - 管理应用级状态
 */
class MainViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    /**
     * 启动目标页面 - 根据 Token 是否存在决定
     */
    val startDestination: StateFlow<String> = tokenManager.currentStudentId
        .map { id ->
            if (id.isNullOrEmpty()) {
                Screen.Login.route
            } else {
                Screen.Main.route
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Screen.Login.route
        )
}
