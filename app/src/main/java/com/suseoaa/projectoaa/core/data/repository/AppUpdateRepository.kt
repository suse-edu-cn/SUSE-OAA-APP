package com.suseoaa.projectoaa.core.data.repository


import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.suseoaa.projectoaa.BuildConfig
import com.suseoaa.projectoaa.core.network.GithubApiService
import com.suseoaa.projectoaa.core.network.model.update.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class AppUpdateRepository @Inject constructor(
    private val api: GithubApiService,
    @ApplicationContext private val context: Context
) {
    // 替换为目标地址的GitHub 用户名和仓库名
    private val OWNER = "HuangZhuoRui"
    private val REPO = "SUSE-OAA-APP"

    // 用于记录当前正在下载的任务 ID
    var currentDownloadId: Long = -1L
        private set

    suspend fun checkUpdate(): Result<GithubRelease?> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestRelease(OWNER, REPO)
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null) {
                    val remoteVersion = release.tagName.removePrefix("v")
                    val currentVersion = BuildConfig.VERSION_NAME

                    if (compareVersions(remoteVersion, currentVersion) > 0) {
                        return@withContext Result.success(release)
                    }
                }
                return@withContext Result.success(null) // 无更新
            } else {
                return@withContext Result.failure(Exception("检查更新失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // 版本号比较逻辑 (1.0.1 > 1.0.0)
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

    // downloadApk 方法，记录 ID
    fun downloadApk(url: String, fileName: String): Long {
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

        // 记录 ID
        currentDownloadId = id
        return id
    }

    // 根据 DownloadID 触发安装
    fun installApkById(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        // Android 8.0+ 检查权限
        if (!context.packageManager.canRequestPackageInstalls()) {
            // 如果没有权限，跳转到设置页面
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = "package:${context.packageName}".toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return // 暂停安装，等用户授权
        }
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            // 授权安装程序读取 URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}