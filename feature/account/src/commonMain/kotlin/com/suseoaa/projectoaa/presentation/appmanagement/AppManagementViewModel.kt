package com.suseoaa.projectoaa.presentation.appmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp
import com.suseoaa.projectoaa.shared.domain.repository.AppManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppManagementUiState(
    val apps: List<OaaApp> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val createdSecretApp: OaaApp? = null,
    val message: String? = null
)

/**
 * OAuth 应用管理（`/app` 接口）：仅管理员（level >= 80）可见。
 */
class AppManagementViewModel(
    private val appManagementRepository: AppManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppManagementUiState())
    val uiState: StateFlow<AppManagementUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            appManagementRepository.listApps()
                .onSuccess { apps ->
                    _uiState.update { it.copy(isLoading = false, apps = apps) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = error.message ?: "获取应用列表失败"
                        )
                    }
                }
        }
    }

    fun createApp(clientId: String, name: String, redirectUris: String, scope: String) {
        if (_uiState.value.isSaving) return
        val cleanClientId = clientId.trim()
        val cleanName = name.trim()
        val cleanRedirects = redirectUris.trim()
        val cleanScope = normalizeScopes(scope)
        val validationError = validate(cleanClientId, cleanName, cleanRedirects)
        if (validationError != null) {
            _uiState.update { it.copy(message = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            appManagementRepository.createApp(
                clientId = cleanClientId,
                name = cleanName,
                redirectUris = cleanRedirects,
                scope = cleanScope
            ).onSuccess { created ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        createdSecretApp = created.takeIf { app -> app.clientSecret.isNotBlank() },
                        message = "应用已创建"
                    )
                }
                refresh()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isSaving = false, message = error.message ?: "创建应用失败")
                }
            }
        }
    }

    fun updateApp(
        clientId: String,
        name: String,
        redirectUris: String,
        scope: String,
        isActive: Boolean
    ) {
        if (_uiState.value.isSaving) return
        val cleanClientId = clientId.trim()
        val cleanName = name.trim()
        val cleanRedirects = redirectUris.trim()
        val cleanScope = normalizeScopes(scope)
        val validationError = validate(cleanClientId, cleanName, cleanRedirects)
        if (validationError != null) {
            _uiState.update { it.copy(message = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            appManagementRepository.updateApp(
                clientId = cleanClientId,
                name = cleanName,
                redirectUris = cleanRedirects,
                scope = cleanScope,
                isActive = isActive
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "应用已更新") }
                refresh()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isSaving = false, message = error.message ?: "修改应用失败")
                }
            }
        }
    }

    fun deleteApp(clientId: String) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            appManagementRepository.deleteApp(clientId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            apps = state.apps.filterNot { it.clientId == clientId },
                            message = "应用已删除"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, message = error.message ?: "删除应用失败")
                    }
                }
        }
    }

    fun dismissCreatedSecret() {
        _uiState.update { it.copy(createdSecretApp = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun validate(clientId: String, name: String, redirectUris: String): String? = when {
        clientId.isBlank() -> "Client ID 不能为空"
        name.isBlank() -> "应用名称不能为空"
        redirectUris.isBlank() -> "回调地址不能为空"
        else -> null
    }

    private fun normalizeScopes(raw: String): String =
        raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct().joinToString(" ")
}
