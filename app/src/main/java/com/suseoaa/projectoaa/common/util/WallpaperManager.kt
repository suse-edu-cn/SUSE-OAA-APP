package com.suseoaa.projectoaa.common.util

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat // 新增
import com.suseoaa.projectoaa.common.network.LoliconApi
import com.suseoaa.projectoaa.common.network.LoliconData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WallpaperManager {
    // ... 其他变量保持不变 ...
    var currentWallpaperUri = mutableStateOf<Uri?>(null)
        private set

    private const val PREF_NAME = "wallpaper_prefs"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val CACHE_DIR_NAME = "anime_wallpapers"
    private const val MAX_CACHE_SIZE = 5
    private const val UPDATE_INTERVAL_MS = 2 * 24 * 60 * 60 * 1000L

    private val api = LoliconApi.create()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(context: Context) {
        randomizeDisplay(context)
        scope.launch { smartUpdateCache(context) }
    }

    fun randomizeDisplay(context: Context) {
        // ... 保持不变 ...
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (cacheDir.exists()) {
            val files = cacheDir.listFiles()?.toList() ?: emptyList()
            if (files.isNotEmpty()) {
                val randomFile = files.random()
                scope.launch(Dispatchers.Main) {
                    if (currentWallpaperUri.value?.path == randomFile.path && files.size > 1) {
                        val otherFiles = files.filter { it.path != randomFile.path }
                        if (otherFiles.isNotEmpty()) {
                            currentWallpaperUri.value = Uri.fromFile(otherFiles.random())
                            return@launch
                        }
                    }
                    currentWallpaperUri.value = Uri.fromFile(randomFile)
                }
            }
        }
    }

    // === 直接调用系统弹窗逻辑 ===
    fun saveCurrentToGallery(context: Context) {
        scope.launch {
            val currentUri = currentWallpaperUri.value
            if (currentUri == null || currentUri.path == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "当前没有壁纸可保存", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val sourceFile = File(currentUri.path!!)
            if (!sourceFile.exists()) return@launch

            try {
                // 1. 尝试执行保存逻辑
                val fileName = "OAA_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectOAA")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "壁纸已保存到相册！", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("无法创建媒体文件")
                }

            } catch (e: Exception) {
                e.printStackTrace()

                // 2. 捕获权限异常，尝试弹出系统弹窗
                if (e is SecurityException && context is Activity) {
                    withContext(Dispatchers.Main) {
                        // 情况 A: Android 10+ Scoped Storage 恢复性弹窗
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                            try {
                                context.startIntentSenderForResult(
                                    e.userAction.actionIntent.intentSender,
                                    1001, null, 0, 0, 0
                                )
                                Toast.makeText(context, "请在弹窗中允许操作后重试", Toast.LENGTH_LONG).show()
                            } catch (ex: Exception) {
                                Toast.makeText(context, "无法唤起系统弹窗", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // 情况 B: Android 9及以下 或 标准权限缺失 -> 请求系统权限弹窗
                        else {
                            ActivityCompat.requestPermissions(
                                context,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                1002
                            )
                            Toast.makeText(context, "请授权存储权限后重试", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // 其他错误或 Context 不是 Activity
                    withContext(Dispatchers.Main) {
                        val msg = if (e is SecurityException) "保存失败：缺少权限" else "保存失败: ${e.message}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ... (smartUpdateCache, fetchAndSaveWallpapers, saveImageToDisk 等私有方法保持不变) ...
    private suspend fun smartUpdateCache(context: Context) {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val files = cacheDir.listFiles()?.toList() ?: emptyList()
        val currentCount = files.size
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()

        try {
            when {
                currentCount < MAX_CACHE_SIZE -> {
                    val needCount = MAX_CACHE_SIZE - currentCount
                    fetchAndSaveWallpapers(context, cacheDir, needCount)
                }
                currentTime - lastUpdate > UPDATE_INTERVAL_MS -> {
                    fetchAndSaveWallpapers(context, cacheDir, 1)
                    removeOldestImages(cacheDir, 1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchAndSaveWallpapers(context: Context, cacheDir: File, count: Int) {
        if (count <= 0) return

        try {
            val orientation = context.resources.configuration.orientation
            val ratioParam = if (orientation == Configuration.ORIENTATION_LANDSCAPE) "gt1" else "lt1"

            val response = api.getSetu(
                r18 = 0,
                excludeAI = true,
                num = count,
                tag = "少女",
                aspectRatio = ratioParam
            )

            val dataList = response.data ?: return
            var successCount = 0

            for (data in dataList) {
                val success = retry(times = 3, delayMs = 1000) {
                    saveImageToDisk(data, cacheDir)
                }
                if (success) successCount++
            }

            if (successCount > 0) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()

                if (currentWallpaperUri.value == null) {
                    randomizeDisplay(context)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveImageToDisk(data: LoliconData, cacheDir: File): Boolean {
        val imageUrl = data.urls["regular"] ?: data.urls["original"] ?: return false
        val fileName = "${data.pid}_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(imageUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext true
                } else false
            } catch (e: Exception) {
                if (file.exists()) file.delete()
                return@withContext false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun removeOldestImages(cacheDir: File, countToRemove: Int) {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var deleted = 0
        for (file in files) {
            if (deleted >= countToRemove) break
            if (file.delete()) deleted++
        }
    }

    private suspend fun retry(times: Int, delayMs: Long, block: suspend () -> Boolean): Boolean {
        repeat(times) {
            if (block()) return true
            delay(delayMs)
        }
        return false
    }
}