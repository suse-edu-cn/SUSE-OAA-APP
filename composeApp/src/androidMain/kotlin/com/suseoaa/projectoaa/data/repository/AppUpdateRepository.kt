package com.suseoaa.projectoaa.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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
            val response: HttpResponse = httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            
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

    /**
     * 根据 DownloadID 触发安装
     */
    actual fun installApkById(downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Android 8.0+ 检查安装未知应用权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = "package:${context.packageName}".toUri()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }

            // 策略1：通过 DownloadManager 查询下载文件的本地路径
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

            // 策略2：回退到下载目录中查找最新 APK
            if (file == null || !file.exists()) {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                file = downloadsDir?.listFiles()
                    ?.filter { it.name.endsWith(".apk") }
                    ?.maxByOrNull { it.lastModified() }
            }

            if (file == null || !file.exists()) return

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
