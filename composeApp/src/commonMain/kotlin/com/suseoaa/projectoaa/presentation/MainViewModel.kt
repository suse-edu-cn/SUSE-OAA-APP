package com.suseoaa.projectoaa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.BackgroundPageIds
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 主 ViewModel - 管理应用级状态
 */
class MainViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _selectedMainTab = MutableStateFlow(0)
    val selectedMainTab: StateFlow<Int> = _selectedMainTab.asStateFlow()

    private val _homeFeatureDrawerExpanded = MutableStateFlow(false)
    val homeFeatureDrawerExpanded: StateFlow<Boolean> = _homeFeatureDrawerExpanded.asStateFlow()

    private val _academicFeatureDrawerExpanded = MutableStateFlow(false)
    val academicFeatureDrawerExpanded: StateFlow<Boolean> = _academicFeatureDrawerExpanded.asStateFlow()

    fun updateSelectedMainTab(index: Int) {
        _selectedMainTab.value = index
    }

    fun updateHomeFeatureDrawerExpanded(expanded: Boolean) {
        _homeFeatureDrawerExpanded.value = expanded
    }

    fun updateAcademicFeatureDrawerExpanded(expanded: Boolean) {
        _academicFeatureDrawerExpanded.value = expanded
    }

    /**
     * 启动目标页面 - 根据软件账号 Token 是否存在决定
     * 使用 tokenFlow (JWT Token) 而不是 currentStudentId (教务系统学号)
     * 初始值为 null，表示正在加载，防止登录页闪烁
     */
    val startDestination: StateFlow<String?> = tokenManager.tokenFlow
        .map { token ->
            if (token.isNullOrEmpty()) {
                Screen.Login.route
            } else {
                Screen.Main.route
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null  // 初始值为 null，表示正在加载
        )

    val dynamicColorEnabled: StateFlow<Boolean> = tokenManager.dynamicColorEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val dynamicPaletteLightColorHex: StateFlow<String?> = tokenManager.dynamicColorPaletteLightFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val dynamicPaletteDarkColorHex: StateFlow<String?> = tokenManager.dynamicColorPaletteDarkFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val appBackgroundImages: StateFlow<Map<String, String?>> = tokenManager.appBackgroundImagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapOf(
                BackgroundPageIds.DEFAULT to null,
                BackgroundPageIds.HOME to null,
                BackgroundPageIds.COURSE to null,
                BackgroundPageIds.ACADEMIC to null,
                BackgroundPageIds.PERSON to null,
            )
        )
}
