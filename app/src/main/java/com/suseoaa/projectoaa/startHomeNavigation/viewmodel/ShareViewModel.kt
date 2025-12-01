package com.suseoaa.projectoaa.navigation.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.navigation.repository.FeedbackRepository
import com.suseoaa.projectoaa.navigation.repository.SettingsRepository
import com.suseoaa.projectoaa.navigation.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val feedbackRepository: FeedbackRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // ==========================================
    // 1. 全局状态流 (Global State Flows)
    // ==========================================

    /**
     * 全局壁纸流 (用于 App 背景)
     * stateIn 确保了 Flow 是热流，配置了 5秒超时停止，节省资源
     */
    val appWallpaper = wallpaperRepository.currentWallpaper
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * 壁纸遮罩透明度 (直接从 Manager 桥接，保持数据源单一)
     */
    val wallpaperAlpha = WallpaperManager.wallpaperAlpha

    /**
     * 一次性事件流 (Toast, Snackbar, 导航跳转等)
     */
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // 用户会话信息快捷访问
    val currentUser: String?
        get() = sessionManager.currentUser
    val currentRole: String?
        get() = sessionManager.currentRole

    // ==========================================
    // 2. 主题控制 (核心优化点)
    // ==========================================

    // 使用 Compose State 以便 UI 层直接读取并响应，无需 collectAsState
    var currentTheme by mutableStateOf(ThemeManager.currentTheme)
        private set

    /**
     * 更新主题
     * 采用“乐观更新”策略：先改 UI，再存数据库，确保零延迟
     */
    fun updateTheme(themeConfig: OaaThemeConfig) {
        // 1. 防抖：如果主题没变，直接返回，避免无效重绘
        if (currentTheme.name == themeConfig.name) return

        // 2. [关键] 立即在主线程更新内存单例和 UI State
        // 这保证了点击按钮的瞬间，App 颜色立即发生变化
        ThemeManager.currentTheme = themeConfig
        currentTheme = themeConfig

        // 3. 异步处理耗时操作 (IO 线程)
        viewModelScope.launch(Dispatchers.IO) {
            // 持久化保存
            settingsRepository.saveThemeName(themeConfig.name)

            // 智能壁纸检测：
            // 如果切到二次元主题且当前无壁纸（例如首次安装或缓存被清），自动触发下载
            // 注意：这里检查 appWallpaper.value 需要由 StateFlow 保证线程安全
            if (themeConfig.name.contains("二次元") && appWallpaper.value == null) {
                wallpaperRepository.refreshWallpaper()
            }
        }
    }

    // ==========================================
    // 3. 设置相关 (UI Toggle)
    // ==========================================

    var notificationEnabled by mutableStateOf(true)
        private set

    fun onNotificationToggleChanged(isEnabled: Boolean) {
        notificationEnabled = isEnabled
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveNotificationEnabled(isEnabled)
        }
    }

    var privacyEnabled by mutableStateOf(false)
        private set

    fun onPrivacyToggleChanged(isEnabled: Boolean) {
        privacyEnabled = isEnabled
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.savePrivacyEnabled(isEnabled)
        }
    }

    // ==========================================
    // 4. 反馈与杂项
    // ==========================================

    var feedbackText by mutableStateOf("")
        private set
    var isSubmittingFeedback by mutableStateOf(false)
        private set

    fun onFeedbackTextChanged(text: String) {
        feedbackText = text
    }

    fun submitFeedback() {
        if (feedbackText.isBlank()) {
            viewModelScope.launch { _toastEvent.emit("反馈内容不能为空") }
            return
        }

        isSubmittingFeedback = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSuccess = feedbackRepository.submit(feedbackText)

                // 切换回主线程更新 UI 状态
                withContext(Dispatchers.Main) {
                    isSubmittingFeedback = false
                    if (isSuccess) {
                        feedbackText = ""
                        _toastEvent.emit("感谢您的反馈！")
                    } else {
                        _toastEvent.emit("提交失败，请重试")
                    }
                }
            } catch (e: Exception) {
                Log.e("ShareViewModel", "Feedback error", e)
                withContext(Dispatchers.Main) {
                    isSubmittingFeedback = false
                    _toastEvent.emit("网络错误，请稍后重试")
                }
            }
        }
    }

    // UI 可见性控制 (例如隐藏敏感/测试 UI)
    private val _showOfState = MutableStateFlow(false)
    val showOfState: StateFlow<Boolean> = _showOfState
    fun toggleOfUI() { _showOfState.value = !_showOfState.value }

    // ==========================================
    // 5. 壁纸操作委托
    // ==========================================

    fun updateWallpaperAlpha(alpha: Float) {
        // 调用 Manager 的静态方法，Manager 内部处理持久化逻辑
        WallpaperManager.setWallpaperAlpha(context, alpha)
    }

    fun onSaveWallpaper() {
        viewModelScope.launch {
            // Manager 内部已处理 IO 线程和 Toast
            wallpaperRepository.saveCurrentToGallery()
        }
    }

    fun onRefreshWallpaper() {
        viewModelScope.launch {
            // Manager 内部已处理 IO 线程和 Toast
            wallpaperRepository.refreshWallpaper()
        }
    }

    // ==========================================
    // 6. 初始化
    // ==========================================

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            // 从数据库/SP 读取设置 (IO 操作)
            val savedThemeName = settingsRepository.getThemeName()

            // 如果找不到保存的主题名，默认使用列表第一个
            val savedTheme = ThemeManager.themeList.find { it.name == savedThemeName }
                ?: ThemeManager.themeList.first()

            val notif = settingsRepository.getNotificationEnabled()
            val privacy = settingsRepository.getPrivacyEnabled()

            // 切换回主线程批量更新 UI 状态，减少重组次数
            withContext(Dispatchers.Main) {
                ThemeManager.currentTheme = savedTheme
                currentTheme = savedTheme
                notificationEnabled = notif
                privacyEnabled = privacy
            }
        }
    }
}