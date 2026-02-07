package com.suseoaa.projectoaa.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS 平台的应用更新仓库实现
 * iOS 不支持直接下载安装 APK，但提供跳转到 App Store 或 TestFlight 的能力
 */
actual class AppUpdateRepository(
    private val httpClient: HttpClient,
    private val json: Json,
    private val currentVersionName: String
) {
    private val OWNER = "HuangZhuoRui"
    private val REPO = "SUSE-OAA-APP"

    actual val currentDownloadId: Long = -1L

    /**
     * 检查是否有新版本
     * iOS 端可以检查 GitHub Release，但仅作提示用途
     */
    actual suspend fun checkUpdate(): Result<GithubRelease?> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse =
                httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases/latest")

            if (response.status.value == 200) {
                val release: GithubRelease = response.body()
                val remoteVersion = release.tagName.removePrefix("v")

                if (compareVersions(remoteVersion, currentVersionName) > 0) {
                    Result.success(release)
                } else {
                    Result.success(null) // 无更新
                }
            } else {
                Result.failure(Exception("检查更新失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 版本号比较逻辑 (1.0.1 > 1.0.0)
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    /**
     * iOS 不支持直接下载 APK
     * 这里可以打开 GitHub Releases 页面或 TestFlight 链接
     */
    actual fun downloadApk(url: String, fileName: String): Long {
        // iOS 打开 GitHub Release 页面让用户手动获取 IPA 或跳转到 TestFlight
        val releaseUrl = "https://github.com/$OWNER/$REPO/releases"
        val nsUrl = NSURL.URLWithString(releaseUrl)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
        return -1L
    }

    /**
     * iOS 不支持根据 DownloadID 安装
     */
    actual fun installApkById(downloadId: Long) {
        // iOS 无法直接安装应用，什么都不做
        // 如果有 TestFlight 链接，可以在这里打开
    }
}
