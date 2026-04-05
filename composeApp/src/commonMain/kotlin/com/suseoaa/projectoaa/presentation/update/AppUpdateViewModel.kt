package com.suseoaa.projectoaa.presentation.update

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.data.repository.GithubRelease
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
@Immutable
data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestRelease: GithubRelease? = null,
    val errorMessage: String? = null,
    val downloadProgress: Int = 0, // 0-100
    val hasShownAutoDialog: Boolean = false, // 是否已经自动弹过窗
    val showDialog: Boolean = false // 是否显示更新对话框
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

    private val _allReleases = MutableStateFlow<List<GithubRelease>>(emptyList())
    val allReleases: StateFlow<List<GithubRelease>> = _allReleases.asStateFlow()

    private val _events = MutableSharedFlow<UpdateEvent>()
    val events: SharedFlow<UpdateEvent> = _events.asSharedFlow()

    private var currentDownloadId: Long = -1L
    private var progressPollingJob: Job? = null
    private var expectedDigest: String? = null
    private var isProxyDownload: Boolean = false

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
                errorMessage = null,
                showDialog = true
            )

            appUpdateRepository.checkUpdate()
                .onSuccess { release ->
                    if (release != null) {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = true,
                            latestRelease = release,
                            showDialog = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = false,
                            showDialog = true
                        )
                        _events.emit(UpdateEvent.NoUpdateAvailable)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "检查更新失败",
                        showDialog = true
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
     * 获取所有历史更新版本
     */
    fun fetchAllReleases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                errorMessage = null
            )
            
            // 如果列表已经有数据，可以先不立即清空，以免闪烁
            appUpdateRepository.getAllReleases()
                .onSuccess { releases ->
                    _allReleases.value = releases
                    _uiState.value = _uiState.value.copy(
                        isChecking = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "获取历史更新失败"
                    )
                    _events.emit(UpdateEvent.ShowToast(error.message ?: "获取历史更新失败"))
                }
        }
    }

    /**
     * 下载指定的 APK
     */
    fun downloadApk(url: String, fileName: String, digest: String? = null, isProxy: Boolean = false) {
        _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0)
        expectedDigest = digest
        isProxyDownload = isProxy

        currentDownloadId = appUpdateRepository.downloadApk(
            url = url,
            fileName = fileName
        )

        if (currentDownloadId == -1L) {
            // iOS 平台不支持直接下载
            _uiState.value = _uiState.value.copy(isDownloading = false)
        } else {
            // 启动进度轮询
            startProgressPolling()
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
    fun startDownload(isProxy: Boolean = false) {
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

        val url = if (isProxy) "https://ghfast.top/${apkAsset.downloadUrl}" else apkAsset.downloadUrl
        
        // 调用我们已经定义好的带代理和 digest 参数的方法
        downloadApk(url, apkAsset.name, apkAsset.digest, isProxy)
    }

    /**
     * 轮询下载进度
     */
    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val progress = appUpdateRepository.getDownloadProgress(currentDownloadId)
                if (progress < 0) break // 查询失败，停止轮询

                _uiState.value = _uiState.value.copy(downloadProgress = progress)

                if (progress >= 100) {
                    if (isProxyDownload && expectedDigest != null) {
                        val isValid = appUpdateRepository.verifyApkSha256(currentDownloadId, expectedDigest!!)
                        if (!isValid) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "安装包已损坏",
                                isDownloading = false,
                                downloadProgress = 0
                            )
                            _events.emit(UpdateEvent.ShowToast("安装包已损坏"))
                            appUpdateRepository.cancelDownload(currentDownloadId)
                            break
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgress = 100
                    )
                    _events.emit(UpdateEvent.DownloadComplete)
                    // 下载完成后自动拉起安装
                    installDownloadedApk()
                    break
                }
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        progressPollingJob?.cancel()
        if (currentDownloadId != -1L) {
            appUpdateRepository.cancelDownload(currentDownloadId)
        }
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadProgress = 0
        )
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
            progressPollingJob?.cancel()
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
     * 关闭更新弹窗（保留更新信息用于红点和版本号显示）
     */
    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false
        )
    }
}
