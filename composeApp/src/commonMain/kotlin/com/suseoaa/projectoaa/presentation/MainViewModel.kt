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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.suseoaa.projectoaa.shared.data.repository.OaaAuthRepository
import com.suseoaa.projectoaa.shared.util.OaaClock

/**
 * 主 ViewModel - 管理应用级状态
 */
class MainViewModel(
    private val tokenManager: TokenManager,
    private val oaaAuthRepository: OaaAuthRepository
) : ViewModel() {

    private val _selectedMainTab = MutableStateFlow(0)
    val selectedMainTab: StateFlow<Int> = _selectedMainTab.asStateFlow()

    private val _homeFeatureDrawerExpanded = MutableStateFlow(false)
    val homeFeatureDrawerExpanded: StateFlow<Boolean> = _homeFeatureDrawerExpanded.asStateFlow()

    private val _academicFeatureDrawerExpanded = MutableStateFlow(false)
    val academicFeatureDrawerExpanded: StateFlow<Boolean> = _academicFeatureDrawerExpanded.asStateFlow()

    init {
        // 启动时读取默认起始页并应用
        viewModelScope.launch {
            val startTab = tokenManager.defaultStartTabFlow.first()
            if (startTab != 0) {
                _selectedMainTab.value = startTab
            }
        }

        // 检查 Token 是否需要刷新
        viewModelScope.launch {
            val isLoggedIn = tokenManager.isLoggedIn.first()
            if (isLoggedIn) {
                val currentStudentId = tokenManager.currentStudentId.first()
                val userPassword = tokenManager.getPasswordSynchronously()

                if (!currentStudentId.isNullOrEmpty() && !userPassword.isNullOrEmpty()) {
                    val lastUpdateTime = tokenManager.getTokenLastUpdateTime()
                    val currentTime = OaaClock.now().toEpochMilliseconds()
                    val tenDaysInMillis = 10L * 24 * 60 * 60 * 1000

                    if (lastUpdateTime == 0L || (currentTime - lastUpdateTime > tenDaysInMillis)) {
                        // 执行一次登录以刷新 local token
                        val result = oaaAuthRepository.login(currentStudentId, userPassword)
                        result.onSuccess { response ->
                            response.data?.token?.let { token ->
                                tokenManager.saveToken(token)
                            }
                            tokenManager.saveTokenLastUpdateTime(currentTime)
                        }
                    }
                }
            }
        }
    }

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

    // 默认起始页（0=首页, 1=课程, 2=教务信息, 3=个人）
    val defaultStartTab: StateFlow<Int> = tokenManager.defaultStartTabFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    fun saveDefaultStartTab(tabIndex: Int) {
        viewModelScope.launch {
            tokenManager.saveDefaultStartTab(tabIndex)
        }
    }
}
