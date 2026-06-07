package com.suseoaa.projectoaa.presentation.ailab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.util.DeviceInfo
import com.suseoaa.projectoaa.util.ModelRecommendation
import com.suseoaa.projectoaa.util.ModelRecommendationLevel
import com.suseoaa.projectoaa.util.PlatformDeviceInfo
import com.suseoaa.projectoaa.util.computeModelRecommendation
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import kotlinx.coroutines.withContext
import com.suseoaa.projectoaa.util.AiModelMetadata
import com.suseoaa.projectoaa.util.AvailableAiModels
import com.suseoaa.projectoaa.util.LocalModelFile
import com.suseoaa.projectoaa.util.ModelDownloader
import com.suseoaa.projectoaa.util.isCompatibleWithDevice

/**
 * 模型下载状态
 */
sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data object CheckingNetwork : ModelDownloadState()
    data object NotOnWifi : ModelDownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long, val speedStr: String = "") : ModelDownloadState()
    data object Downloaded : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

/**
 * AI 实验室 UI 状态
 */
data class AiLabUiState(
    val isLoadingDeviceInfo: Boolean = true,
    val deviceInfo: DeviceInfo? = null,
    val recommendation: ModelRecommendation? = null,
    val selectedModel: AiModelMetadata? = null, // 用户手动选中的覆盖模型
    val availableModels: List<AiModelMetadata> = AvailableAiModels,
    val downloadState: ModelDownloadState = ModelDownloadState.Idle,
    val hasUpdateAvailable: Boolean = false,
    val latestRemoteETag: String? = null,
    val errorMessage: String? = null,
    val localModels: List<LocalModelFile> = emptyList(),
    /** 用户是否巧大GPU推理，默认true（优先GPU） */
    val preferGpu: Boolean = true,
    /** GPU初始化失败后设为true，用于触发Toast提醒 */
    val gpuCrashDetected: Boolean = false
)

class AiLabViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _uiState = MutableStateFlow(AiLabUiState())
    val uiState: StateFlow<AiLabUiState> = _uiState.asStateFlow()


    private val _showTokenDialog = MutableStateFlow(false)
    val showTokenDialog: StateFlow<Boolean> = _showTokenDialog.asStateFlow()

    private var currentKaggleAuth: String? = null
    private var downloadJob: Job? = null
    
    // 我们在这里暂用一个简易 client，或者通过依赖注入获取
    private val httpClient by lazy { HttpClient() }

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDeviceInfo = true) }
            try {
                val info = PlatformDeviceInfo.queryDeviceInfo()
                val available = AvailableAiModels // Assuming utility access
                val compatibleModels = available.filter { it.isCompatibleWithDevice(info) }
                    .ifEmpty { available.filter { it.targetSocModels.isEmpty() } }
                    .ifEmpty { available }
                val rec = computeModelRecommendation(info)
                val localFiles = ModelDownloader.getDownloadedModels()
                
                println("AiLab: localFiles from disk: ${localFiles.map { it.name }}")
                println("AiLab: detected SoC model: ${info.socModel.ifBlank { "<unknown>" }}")
                
                val downloadedAvailableModels = compatibleModels.filter { model ->
                    val fileName = model.downloadUrl.substringAfterLast("/").substringBefore("?")
                    localFiles.any { 
                        it.name == fileName || 
                        it.name == "$fileName.task" || 
                        it.name == "$fileName.bin" ||
                        it.name == "${fileName.removeSuffix(".litertlm")}.bin"
                    }
                }
                
                println("AiLab: matched downloadedAvailableModels: ${downloadedAvailableModels.map { it.name }}")
                
                val savedModelId = tokenManager.aiLabSelectedModelIdFlow.firstOrNull()
                val savedModel = available.firstOrNull { it.id == savedModelId }
                if (savedModel != null && !savedModel.isCompatibleWithDevice(info)) {
                    println(
                        "AiLab: saved model ${savedModel.name} is incompatible with SoC ${info.socModel}; selecting a compatible model instead."
                    )
                }
                val defaultModel = compatibleModels.find { it.recommendedLevel == rec.level }
                
                val selected = if (savedModel != null && savedModel.isCompatibleWithDevice(info)) {
                    savedModel
                } else if (downloadedAvailableModels.isNotEmpty()) {
                    downloadedAvailableModels.first()
                } else {
                    defaultModel ?: compatibleModels.first()
                }
                val selectedFileName = selected.downloadUrl.substringAfterLast("/").substringBefore("?")
                com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setTargetModelFileName(selectedFileName)
                if (savedModelId.isNullOrBlank() || savedModelId != selected.id) {
                    tokenManager.saveAiLabSelectedModelId(selected.id)
                }
                
                println("AiLab: selectedModel determined as: ${selected.name}")
                
                val isDownloaded = ModelDownloader.isModelDownloaded(selected.downloadUrl)
                println("AiLab: isDownloaded for selectedModel: $isDownloaded")
                _uiState.update { 
                    it.copy(
                        isLoadingDeviceInfo = false, 
                        deviceInfo = info, 
                        recommendation = rec, 
                        selectedModel = selected,
                        availableModels = compatibleModels,
                        downloadState = if (isDownloaded) ModelDownloadState.Downloaded else ModelDownloadState.Idle,
                        localModels = localFiles
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDeviceInfo = false,
                        errorMessage = "设备信息读取失败：${e.message}"
                    )
                }
            }
            // 当选择了推荐模型后，检测是否有更新
            checkForUpdates()
        }

        viewModelScope.launch {
            tokenManager.kaggleAuthFlow.collect { auth ->
                currentKaggleAuth = auth
            }
        }
        
        viewModelScope.launch {
            tokenManager.aiLabPreferGpuFlow.collect { preferGpu ->
                _uiState.update { it.copy(preferGpu = preferGpu) }
                com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setPreferGpu(preferGpu)
            }
        }
    }

    /**
     * 用户手动切换模型
     */
    fun selectModel(modelId: String) {
        val model = _uiState.value.availableModels.find { it.id == modelId }
        if (model != null) {
            val isDownloaded = ModelDownloader.isModelDownloaded(model.downloadUrl)
            _uiState.update { 
                it.copy(
                    selectedModel = model, 
                    hasUpdateAvailable = false, 
                    latestRemoteETag = null,
                    downloadState = if (isDownloaded) ModelDownloadState.Downloaded else ModelDownloadState.Idle
                ) 
            }
            viewModelScope.launch {
                tokenManager.saveAiLabSelectedModelId(modelId)
            }
            val fileName = model.downloadUrl.substringAfterLast("/").substringBefore("?")
            com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.setTargetModelFileName(fileName)
            checkForUpdates()
        }
    }

    /**
     * 检查当前选中模型是否有官方更新
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            val targetModel = _uiState.value.selectedModel ?: return@launch
            val localETag = tokenManager.getModelETagFlow(targetModel.id).firstOrNull()
            if (localETag.isNullOrBlank()) return@launch // 未下载过，不检测更新

            val remoteETag = ModelDownloader.getETag(httpClient, targetModel.downloadUrl, currentKaggleAuth)
            if (!remoteETag.isNullOrBlank() && remoteETag != localETag) {
                _uiState.update { it.copy(hasUpdateAvailable = true, latestRemoteETag = remoteETag) }
            }
        }
    }

    fun dismissTokenDialog() {
        _showTokenDialog.value = false
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadLocalModels() {
        val files = ModelDownloader.getDownloadedModels()
        _uiState.update { it.copy(localModels = files) }
    }

    fun deleteLocalModel(fileName: String) {
        val success = ModelDownloader.deleteModel(fileName)
        if (success) {
            loadLocalModels()
            // 重新检查当前选中的模型是否已下载
            val targetModel = _uiState.value.selectedModel
            if (targetModel != null) {
                val isDownloaded = ModelDownloader.isModelDownloaded(targetModel.downloadUrl)
                _uiState.update { 
                    it.copy(downloadState = if (isDownloaded) ModelDownloadState.Downloaded else ModelDownloadState.Idle)
                }
            }
        }
    }

    fun submitKaggleAuthAndDownload(username: String, key: String) {
        viewModelScope.launch {
            val authBase64 = "$username:$key".encodeBase64()
            tokenManager.saveKaggleAuth(authBase64)
            currentKaggleAuth = authBase64 // 立即同步更新内存状态，避免 Flow 收集的延迟
            _showTokenDialog.value = false
            startDownload() // 凭证保存后，继续执行下载
        }
    }

    /**
     * 开始下载模型
     */
    fun startDownload() {
        if (downloadJob?.isActive == true) {
            _uiState.update { it.copy(errorMessage = "当前已有下载任务正在进行，请稍候") }
            return
        }
        val targetModel = _uiState.value.selectedModel
        if (targetModel == null) {
            _uiState.update { it.copy(errorMessage = "未获取到模型信息，无法下载") }
            return
        }

        _uiState.update {
            it.copy(
                downloadState = ModelDownloadState.Downloading(
                    progress = 0f, downloadedBytes = 0L, totalBytes = 1L, speedStr = "0 B/s"
                )
            )
        }

        downloadJob = viewModelScope.launch {
            var lastUpdate = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
            var lastBytes = 0L

            val (success, etag) = ModelDownloader.downloadModel(httpClient, targetModel.downloadUrl, currentKaggleAuth) { downloaded, total ->
                val now = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
                val diffTime = now - lastUpdate
                if (diffTime >= 500) {
                    val diffBytes = downloaded - lastBytes
                    val speedMbps = (diffBytes.toDouble() / 1024 / 1024) / (diffTime / 1000.0)
                    val speedStr = "%.1f MB/s".format(speedMbps)
                    
                    val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    _uiState.update {
                        it.copy(
                            downloadState = ModelDownloadState.Downloading(progress, downloaded, total, speedStr)
                        )
                    }

                    lastUpdate = now
                    lastBytes = downloaded
                }
            }

            if (success) {
                if (etag != null) {
                    tokenManager.saveModelETag(targetModel.id, etag)
                }
                _uiState.update { it.copy(downloadState = ModelDownloadState.Downloaded, hasUpdateAvailable = false, latestRemoteETag = null) }
            } else {
                val errorMsg = when {
                    etag?.contains("401") == true -> "401: 鉴权凭证无效，请检查"
                    etag?.contains("403") == true -> "403: 您需要前往社区网站同意协议"
                    etag?.contains("404") == true -> "404: 官方尚未发布此模型或地址失效"
                    else -> "网络连接异常或已取消 ($etag)"
                }
                val fullError = "下载失败 - $errorMsg"
                _uiState.update { it.copy(downloadState = ModelDownloadState.Error(fullError), errorMessage = fullError) }
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(downloadState = ModelDownloadState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    /**
     * 用户在弹窗中切换GPU/CPU偏好时调用。
     * 不需要重新下载模型，下次调用loadModel()时会自动采用新的Backend配置。
     */
    fun setPreferGpu(prefer: Boolean) {
        _uiState.update { it.copy(preferGpu = prefer) }
        viewModelScope.launch {
            tokenManager.saveAiLabPreferGpu(prefer)
        }
    }

    /**
     * GPU崩溃Toast弹出后由UI调用此函数，将gpuCrashDetected重置为false以避免重复弹出。
     */
    fun dismissGpuCrashNotification() {
        _uiState.update { it.copy(gpuCrashDetected = false) }
    }

    /**
     * 通知ViewModel GPU后端初始化失败，触发Toast提醒用户切换CPU模式。
     */
    fun reportGpuCrash() {
        _uiState.update { it.copy(gpuCrashDetected = true, preferGpu = false) }
    }
}
