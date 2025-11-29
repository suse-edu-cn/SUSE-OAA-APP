package com.suseoaa.projectoaa.navigation.viewmodel

import androidx.lifecycle.ViewModel
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShareViewModel : ViewModel() {

    // === 主题状态控制 ===
    val currentTheme: OaaThemeConfig
        get() = ThemeManager.currentTheme

    // 更新主题的方法，从 ThemeConfig 列表中选择
    fun updateTheme(themeConfig: OaaThemeConfig) {
        ThemeManager.currentTheme = themeConfig
    }

    // === UI 可见性控制 ===
    private val _showOfState = MutableStateFlow(false)
    val showOfState: StateFlow<Boolean> = _showOfState

    fun toggleOfUI() {
        _showOfState.value = !_showOfState.value
    }
}