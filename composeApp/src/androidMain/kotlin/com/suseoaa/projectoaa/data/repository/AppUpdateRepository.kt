package com.suseoaa.projectoaa.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import java.security.MessageDigest
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android 平台的应用更新仓库实现
 */
actual class AppUpdateRepository(
    private val context: Context,
    private val httpClient: HttpClient,
    private val json: Json,
    private val currentVersionName: String
) {
    private val OWNER = "HuangZhuoRui"
    private val REPO = "SUSE-OAA-APP"

    private var _currentDownloadId: Long = -1L
    actual val currentDownloadId: Long
        get() = _currentDownloadId

    /**
     * 检查是否有新版本
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
            val response: HttpResponse = httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases")
            
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
     * 下载 APK
     */
    actual fun downloadApk(url: String, fileName: String): Long {
        // 删除旧文件避免重复
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(url.toUri())
            .setTitle("正在下载新版本")
            .setDescription("正在下载 $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downloadManager.enqueue(request)

        _currentDownloadId = id
        return id
    }

    /**
     * 查询下载进度 (0-100)，-1 表示查询失败
     */
    actual fun getDownloadProgress(downloadId: Long): Int {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                cursor.close()
                return 100
            }
            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()
            return if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
        }
        cursor?.close()
        return -1
    }

    /**
     * 取消下载
     */
    actual fun cancelDownload(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
        _currentDownloadId = -1L
    }

    private fun getDownloadedFile(downloadId: Long): File? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var file: File? = null
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val localUri = cursor.getString(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                )
                cursor.close()
                if (localUri != null) {
                    val path = localUri.removePrefix("file://")
                    val f = File(path)
                    if (f.exists()) file = f
                }
            } else {
                cursor?.close()
            }
        } catch (_: Exception) { /* 某些国产 ROM 可能查询失败 */ }

        if (file == null || !file.exists()) {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            file = downloadsDir?.listFiles()
                ?.filter { it.name.endsWith(".apk") }
                ?.maxByOrNull { it.lastModified() }
        }
        return if (file != null && file.exists()) file else null
    }

    /**
     * 校验下载文件的 SHA-256
     */
    actual suspend fun verifyApkSha256(downloadId: Long, expectedHash: String): Boolean = withContext(Dispatchers.IO) {
        val file = getDownloadedFile(downloadId) ?: return@withContext false
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read = fis.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            val hashBytes = digest.digest()
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            val expected = expectedHash.removePrefix("sha256:").lowercase()
            return@withContext hashString.lowercase() == expected
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 根据 DownloadID 触发安装
     */
    actual fun installApkById(downloadId: Long) {
        try {
            // Android 8.0+ 检查安装未知应用权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = "package:${context.packageName}".toUri()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }

            val file = getDownloadedFile(downloadId)
            if (file == null) return

            // 使用 FileProvider 创建安全的 content:// URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
