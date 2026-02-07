package com.suseoaa.projectoaa.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.data.repository.GithubRelease
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用更新 UI 状态
 */
data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestRelease: GithubRelease? = null,
    val errorMessage: String? = null,
    val downloadProgress: Int = 0, // 0-100
    val hasShownAutoDialog: Boolean = false // 是否已经自动弹过窗
)

/**
 * 更新事件（一次性事件）
 */
sealed class UpdateEvent {
    data class ShowToast(val message: String) : UpdateEvent()
    data object DownloadComplete : UpdateEvent()
    data object NoUpdateAvailable : UpdateEvent()
}

/**
 * 平台类型
 */
expect fun isIosPlatform(): Boolean

/**
 * 获取应用版本号
 */
expect fun getAppVersionName(): String

/**
 * 应用更新 ViewModel
 */
class AppUpdateViewModel(
    private val appUpdateRepository: AppUpdateRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UpdateEvent>()
    val events: SharedFlow<UpdateEvent> = _events.asSharedFlow()

    private var currentDownloadId: Long = -1L

    /**
     * 是否是 iOS 平台
     */
    val isIos: Boolean = isIosPlatform()

    /**
     * 检查更新（用于手动触发，不检查是否已弹窗）
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                errorMessage = null
            )

            appUpdateRepository.checkUpdate()
                .onSuccess { release ->
                    if (release != null) {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = true,
                            latestRelease = release
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = false
                        )
                        _events.emit(UpdateEvent.NoUpdateAvailable)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "检查更新失败"
                    )
                    _events.emit(UpdateEvent.ShowToast(error.message ?: "检查更新失败"))
                }
        }
    }

    /**
     * 检查更新（自动触发，会检查是否已弹窗）
     * 只有在该版本还未弹过窗时才会弹窗
     */
    fun checkForUpdateAuto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                errorMessage = null
            )

            appUpdateRepository.checkUpdate()
                .onSuccess { release ->
                    if (release != null) {
                        // 检查是否已经为这个版本弹过窗
                        val hasShown = tokenManager.hasShownUpdateDialogForVersion(release.tagName)

                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = true,
                            latestRelease = release,
                            hasShownAutoDialog = hasShown
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "检查更新失败"
                    )
                }
        }
    }

    /**
     * 标记已经显示过更新弹窗
     */
    fun markDialogShown() {
        val version = _uiState.value.latestRelease?.tagName ?: return
        viewModelScope.launch {
            tokenManager.markUpdateDialogShown(version)
            _uiState.value = _uiState.value.copy(hasShownAutoDialog = true)
        }
    }

    /**
     * 开始下载更新
     */
    fun startDownload() {
        val release = _uiState.value.latestRelease ?: return

        // 查找 APK 资源
        val apkAsset = release.assets.firstOrNull {
            it.name.endsWith(".apk")
        }

        if (apkAsset == null) {
            viewModelScope.launch {
                _events.emit(UpdateEvent.ShowToast("未找到 APK 下载链接"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isDownloading = true)

        currentDownloadId = appUpdateRepository.downloadApk(
            url = apkAsset.downloadUrl,
            fileName = apkAsset.name
        )

        if (currentDownloadId == -1L) {
            // iOS 平台不支持直接下载
            _uiState.value = _uiState.value.copy(isDownloading = false)
        }
    }

    /**
     * 安装已下载的 APK
     */
    fun installDownloadedApk() {
        if (currentDownloadId == -1L) {
            currentDownloadId = appUpdateRepository.currentDownloadId
        }

        if (currentDownloadId != -1L) {
            appUpdateRepository.installApkById(currentDownloadId)
        }
    }

    /**
     * 处理下载完成回调
     */
    fun onDownloadComplete(downloadId: Long) {
        if (downloadId == currentDownloadId) {
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                downloadProgress = 100
            )
            viewModelScope.launch {
                _events.emit(UpdateEvent.DownloadComplete)
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 关闭更新弹窗
     */
    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(
            hasUpdate = false,
            latestRelease = null
        )
    }
}
