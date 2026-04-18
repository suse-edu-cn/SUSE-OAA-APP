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
                val latestRelease: GithubRelease = response.body()
                val remoteVersion = latestRelease.tagName.removePrefix("v")

                if (compareVersions(remoteVersion, currentVersionName) > 0) {
                    val mergedRelease = mergeMissedReleaseLogs(latestRelease)
                    Result.success(mergedRelease)
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
     * 如果落后多个版本，将中间版本日志追加到最新版日志后
     */
    private suspend fun mergeMissedReleaseLogs(latestRelease: GithubRelease): GithubRelease {
        return try {
            val releasesResponse: HttpResponse =
                httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases")
            if (releasesResponse.status.value != 200) {
                return latestRelease
            }

            val releases: List<GithubRelease> = releasesResponse.body()
            val latestVersion = latestRelease.tagName.removePrefix("v")
            val missedReleases = releases.filter { release ->
                val version = release.tagName.removePrefix("v")
                compareVersions(version, currentVersionName) > 0 &&
                    compareVersions(version, latestVersion) <= 0
            }

            if (missedReleases.size <= 1) {
                return latestRelease
            }

            val missedBodies = missedReleases
                .filter { it.tagName != latestRelease.tagName }
                .mapNotNull { release ->
                    val body = release.body.trim()
                    if (body.isBlank()) null else body
                }

            if (missedBodies.isEmpty()) {
                return latestRelease
            }

            val latestBody = latestRelease.body.trim()
            val mergedBody = buildString {
                if (latestBody.isNotBlank()) {
                    append(latestBody)
                    if (missedBodies.isNotEmpty()) {
                        append("\n\n")
                    }
                }
                append(missedBodies.joinToString("\n\n"))
            }

            latestRelease.copy(body = mergedBody)
        } catch (_: Exception) {
            latestRelease
        }
    }

    /**
     * 获取历史 Release
     */
    actual suspend fun getAllReleases(): Result<List<GithubRelease>> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse =
                httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases")

            if (response.status.value == 200) {
                val releases: List<GithubRelease> = response.body()
                Result.success(releases)
            } else {
                Result.failure(Exception("获取历史版本失败: ${response.status.value}"))
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
     * iOS 不支持下载进度查询
     */
    actual fun getDownloadProgress(downloadId: Long): Int {
        return -1
    }

    /**
     * iOS 不支持取消下载
     */
    actual fun cancelDownload(downloadId: Long) {
        // no-op
    }

    /**
     * iOS 不支持根据 DownloadID 安装
     */
    actual fun installApkById(downloadId: Long) {
        // iOS 无法直接安装应用，什么都不做
        // 如果有 TestFlight 链接，可以在这里打开
    }

    /**
     * iOS 不支持校验 SHA-256
     */
    actual suspend fun verifyApkSha256(downloadId: Long, expectedHash: String): Boolean {
        return false
    }
}
