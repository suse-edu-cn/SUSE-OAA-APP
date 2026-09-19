package com.suseoaa.projectoaa.data.repository

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release 数据模型
 */
@Serializable
@Immutable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,           // 版本号，如 "v1.0.1"
    @SerialName("body")
    val body: String,              // 更新日志
    @SerialName("assets")
    val assets: List<GithubAsset>
)

@Serializable
@Immutable
data class GithubAsset(
    @SerialName("browser_download_url")
    val downloadUrl: String,       // APK 下载链接
    @SerialName("name")
    val name: String,              // 文件名
    @SerialName("digest")
    val digest: String? = null     // SHA256 形如 sha256:xxx
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
     * 校验下载文件的 SHA-256 (仅 Android)
     * @param downloadId 下载任务ID
     * @param expectedHash 预期的 SHA256 值 (格式如 "sha256:xxx...")
     * @return true 校验通过或无需校验，false 校验不通过
     */
    suspend fun verifyApkSha256(downloadId: Long, expectedHash: String): Boolean

    /**
     * 获取当前下载任务ID
     */
    val currentDownloadId: Long

    /**
     * 查询下载进度
     * @return 0-100 的进度值，-1 表示不支持或查询失败
     */
    fun getDownloadProgress(downloadId: Long): Int

    /**
     * 取消下载
     */
    fun cancelDownload(downloadId: Long)
    /**
     * 获取所有历史 Release
     */
    suspend fun getAllReleases(): Result<List<GithubRelease>>
}
