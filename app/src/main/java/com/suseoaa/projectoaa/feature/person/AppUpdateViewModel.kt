package com.suseoaa.projectoaa.feature.person

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.core.network.model.update.GithubRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository
) : ViewModel() {

    // 控制弹窗显示
    var showDialog by mutableStateOf(false)

    // 更新信息数据
    var updateInfo by mutableStateOf<GithubRelease?>(null)

    // 弹窗内的状态文字
    var checkStatus by mutableStateOf("")

    // *标记是否有新版本 (用于 UI 显示红点)
    var hasNewVersion by mutableStateOf(false)
    // 最新版本号字符串 (用于 UI 显示)
    var latestVersionName by mutableStateOf("")

    /**
     * 检查更新
     * @param isManual true=手动点击(无更新也要弹窗), false=自动检查(仅有更新才弹窗)
     */
    fun checkUpdate(isManual: Boolean) {
        viewModelScope.launch {
            if (isManual) {
                checkStatus = "正在检查..."
                showDialog = true // 手动检查立即显示弹窗
            }

            repository.checkUpdate()
                .onSuccess { release ->
                    if (release != null) {
                        // 发现新版本
                        updateInfo = release
                        hasNewVersion = true
                        latestVersionName = release.tagName
                        checkStatus = ""

                        // 无论是手动还是自动，发现新版本都弹窗提醒
                        // (如果不希望自动检查打扰用户，可以将此处改为 if (isManual) showDialog = true)
                        showDialog = true
                    } else {
                        // 没有新版本
                        // 只有是手动检查时，才重置状态并提示“已是最新”
                        // 自动检查如果没发现更新，就什么都不做 (Silent)
                        if (isManual) {
                            checkStatus = "当前已是最新版本"
                            updateInfo = null
                            // 是否要在手动检查确认无更新后消除红点？通常保持原状或根据业务定
                             hasNewVersion = false
                        }
                    }
                }
                .onFailure {
                    if (isManual) {
                        checkStatus = "检查失败: ${it.message}"
                        updateInfo = null
                    }
                }
        }
    }

    fun downloadAndInstall() {
        val info = updateInfo ?: return
        val asset = info.assets.find { it.name.endsWith(".apk") } ?: return
        repository.downloadApk(asset.downloadUrl, asset.name)
        showDialog = false
    }
}