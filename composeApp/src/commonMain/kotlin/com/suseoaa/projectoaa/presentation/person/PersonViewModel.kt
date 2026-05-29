package com.suseoaa.projectoaa.presentation.person

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PersonUiState(
    val isLoading: Boolean = false,
    val userInfo: PersonData? = null,
    val isLoggedOut: Boolean = false,
    val message: String? = null,
    val isCheckinUnlocked: Boolean = false,  // 652签到功能是否已解锁
    val isDynamicColorEnabled: Boolean = false,
    val dynamicPaletteLightColorHex: String? = null,
    val dynamicPaletteDarkColorHex: String? = null,
    val defaultStartTab: Int = 0,  // 默认起始页（0=首页, 1=课程, 2=教务信息, 3=个人）
    val isPredictiveBackEnabled: Boolean = true,
    val isLiquidGlassTabbarEnabled: Boolean = false
)

class PersonViewModel(
    private val personRepository: PersonRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        loadCheckinUnlockStatus()
        loadDynamicColorStatus()
        loadDynamicPaletteColors()
        loadDefaultStartTab()
        loadPredictiveBackEnabled()
        loadLiquidGlassTabbarEnabled()
    }

    private fun loadDynamicColorStatus() {
        viewModelScope.launch {
            tokenManager.dynamicColorEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            val currentState = _uiState.value.isDynamicColorEnabled
            tokenManager.saveDynamicColorEnabled(!currentState)
        }
    }

    private fun loadDynamicPaletteColors() {
        viewModelScope.launch {
            tokenManager.dynamicColorPaletteLightFlow.collect { colorHex ->
                _uiState.update { it.copy(dynamicPaletteLightColorHex = colorHex) }
            }
        }

        viewModelScope.launch {
            tokenManager.dynamicColorPaletteDarkFlow.collect { colorHex ->
                _uiState.update { it.copy(dynamicPaletteDarkColorHex = colorHex) }
            }
        }
    }

    fun setDynamicPaletteColors(lightColorHex: String?, darkColorHex: String?) {
        viewModelScope.launch {
            tokenManager.saveDynamicColorPalettes(lightColorHex, darkColorHex)
        }
    }

    /**
     * 加载652签到功能解锁状态
     */
    private fun loadCheckinUnlockStatus() {
        viewModelScope.launch {
            tokenManager.checkinUnlockedFlow.collect { unlocked ->
                _uiState.update { it.copy(isCheckinUnlocked = unlocked) }
            }
        }
    }

    /**
     * 解锁652签到功能（永久保存）
     */
    fun unlockCheckinFeature() {
        viewModelScope.launch {
            tokenManager.unlockCheckinFeature()
            _uiState.update { it.copy(isCheckinUnlocked = true) }
        }
    }

    private fun loadDefaultStartTab() {
        viewModelScope.launch {
            tokenManager.defaultStartTabFlow.collect { tab ->
                _uiState.update { it.copy(defaultStartTab = tab) }
            }
        }
    }

    fun saveDefaultStartTab(tabIndex: Int) {
        viewModelScope.launch {
            tokenManager.saveDefaultStartTab(tabIndex)
            _uiState.update { it.copy(defaultStartTab = tabIndex) }
        }
    }

    private fun loadPredictiveBackEnabled() {
        viewModelScope.launch {
            tokenManager.predictiveBackEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isPredictiveBackEnabled = enabled) }
            }
        }
    }

    fun togglePredictiveBackEnabled() {
        viewModelScope.launch {
            val currentState = _uiState.value.isPredictiveBackEnabled
            tokenManager.savePredictiveBackEnabled(!currentState)
        }
    }

    private fun loadLiquidGlassTabbarEnabled() {
        viewModelScope.launch {
            tokenManager.liquidGlassTabbarEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isLiquidGlassTabbarEnabled = enabled) }
            }
        }
    }

    fun toggleLiquidGlassTabbarEnabled() {
        viewModelScope.launch {
            val currentState = _uiState.value.isLiquidGlassTabbarEnabled
            tokenManager.saveLiquidGlassTabbarEnabled(!currentState)
        }
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = personRepository.getPersonInfo()

            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userInfo = data
                    )
                }
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            personRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun updateInfo(username: String, name: String, email: String) {
        if (username.isBlank() || name.isBlank() || email.isBlank()) {
            _uiState.update { it.copy(message = "填写的内容不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = personRepository.updateUserInfo(username, name, email)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "信息更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = e.message
                    )
                }
            }
        }
    }

    fun uploadAvatar(imageData: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在上传头像...") }

            val result = personRepository.uploadAvatar(imageData)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "头像更新成功"
                    )
                }
                // 重新加载用户信息
                loadUserInfo()
            }

            result.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "头像上传失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
