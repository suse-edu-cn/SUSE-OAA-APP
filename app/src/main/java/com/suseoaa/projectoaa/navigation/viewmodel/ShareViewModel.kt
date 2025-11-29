package com.suseoaa.projectoaa.navigation.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    // === 设置页 ===

    // 通知管理
    var notificationEnabled by mutableStateOf(true)
        private set
    fun onNotificationToggleChanged(isEnabled: Boolean) {
        notificationEnabled = isEnabled
        // 可以添加调用 Repository 保存设置
    }

    // 隐私设置
    var privacyEnabled by mutableStateOf(false)
        private set
    fun onPrivacyToggleChanged(isEnabled: Boolean) {
        privacyEnabled = isEnabled
    }

    // 反馈问题
    var feedbackText by mutableStateOf("")
        private set
    var isSubmittingFeedback by mutableStateOf(false)
        private set

    fun onFeedbackTextChanged(text: String) {
        feedbackText = text
    }

    fun submitFeedback(context: Context) {
        if (feedbackText.isBlank()) {
            Toast.makeText(context, "反馈内容不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmittingFeedback = true
        // 模拟  实际需要接入API
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(1500)
            isSubmittingFeedback = false
            feedbackText = "" // 清空
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "感谢您的反馈！", Toast.LENGTH_SHORT).show()
            }
        }
    }
}