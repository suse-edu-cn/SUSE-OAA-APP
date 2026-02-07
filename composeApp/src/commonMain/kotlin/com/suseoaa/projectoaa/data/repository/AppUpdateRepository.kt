package com.suseoaa.projectoaa.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release 数据模型
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,           // 版本号，如 "v1.0.1"
    @SerialName("body")
    val body: String,              // 更新日志
    @SerialName("assets")
    val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    @SerialName("browser_download_url")
    val downloadUrl: String,       // APK 下载链接
    @SerialName("name")
    val name: String               // 文件名
)

/**
 * 应用更新仓库接口
 * 使用 expect/actual 模式处理平台差异
 */
expect class AppUpdateRepository {
    /**
     * 检查是否有新版本
     * @return 如果有新版本返回 GithubRelease，否则返回 null
     */
    suspend fun checkUpdate(): Result<GithubRelease?>

    /**
     * 下载 APK (仅 Android)
     * @return 下载任务ID
     */
    fun downloadApk(url: String, fileName: String): Long

    /**
     * 根据下载ID安装 APK (仅 Android)
     */
    fun installApkById(downloadId: Long)

    /**
     * 获取当前下载任务ID
     */
    val currentDownloadId: Long
}
