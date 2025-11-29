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
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
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
    var currentWallpaperUri = mutableStateOf<Uri?>(null)
        private set

    // [修复] 移除了 API_KEY，因为你验证了不需要
    // private const val API_KEY = "..."

    private const val TAG = "WallpaperManager"
    private const val PREF_NAME = "wallpaper_prefs"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val CACHE_DIR_NAME = "anime_wallpapers"
    private const val MAX_CACHE_SIZE = 5
    private const val UPDATE_INTERVAL_MS = 12 * 60 * 60 * 1000L

    private var refreshClickCount = 0
    private val api = LoliconApi.create()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(context: Context) {
        randomizeDisplay(context)
        scope.launch { smartUpdateCache(context) }
    }

    /**
     * 刷新壁纸 (7次点击强制刷新逻辑)
     */
    fun refreshWallpaper(context: Context) {
        refreshClickCount++

        val nextFile = randomizeDisplay(context)

        scope.launch {
            if (refreshClickCount >= 7) {
                refreshClickCount = 0
                Log.d(TAG, "Click count reached 7: Triggering forced cache cleanup")
                forceCleanAndUpdate(context, keepFile = nextFile)
            } else {
                smartUpdateCache(context)
            }
        }

        Toast.makeText(context, "正在刷新壁纸...", Toast.LENGTH_SHORT).show()
    }

    /**
     * 随机切换壁纸并返回选中的文件
     */
    fun randomizeDisplay(context: Context): File? {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (cacheDir.exists()) {
            val files = cacheDir.listFiles()?.toList() ?: emptyList()
            if (files.isNotEmpty()) {
                var randomFile = files.random()
                if (currentWallpaperUri.value?.path == randomFile.path && files.size > 1) {
                    val otherFiles = files.filter { it.path != randomFile.path }
                    if (otherFiles.isNotEmpty()) {
                        randomFile = otherFiles.random()
                    }
                }
                scope.launch(Dispatchers.Main) {
                    currentWallpaperUri.value = Uri.fromFile(randomFile)
                    Log.d(TAG, "Switched wallpaper to: ${randomFile.name}")
                }
                return randomFile
            } else {
                Log.d(TAG, "Cache is empty, cannot display wallpaper")
            }
        }
        return null
    }

    /**
     * 强制清理并更新
     */
    private suspend fun forceCleanAndUpdate(context: Context, keepFile: File?) {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val files = cacheDir.listFiles()
        var deletedCount = 0
        files?.forEach { file ->
            if (keepFile == null || file.absolutePath != keepFile.absolutePath) {
                if (file.delete()) deletedCount++
            }
        }
        Log.d(TAG, "Forced Cleanup: Deleted $deletedCount images. Kept: ${keepFile?.name ?: "None"}")
        fetchAndSaveWallpapers(context, cacheDir, MAX_CACHE_SIZE)
    }

    /**
     * 保存当前壁纸到相册
     */
    fun saveCurrentToGallery(context: Context) {
        scope.launch {
            val currentUri = currentWallpaperUri.value
            if (currentUri == null || currentUri.path == null) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "当前没有壁纸可保存", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            val sourceFile = File(currentUri.path!!)
            if (!sourceFile.exists()) return@launch
            try {
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
                    resolver.openOutputStream(uri)?.use { output -> sourceFile.inputStream().use { input -> input.copyTo(output) } }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear(); contentValues.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "壁纸已保存到相册！", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                handleSaveException(context, e)
            }
        }
    }

    private suspend fun handleSaveException(context: Context, e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 智能更新缓存
     */
    private suspend fun smartUpdateCache(context: Context) {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val files = cacheDir.listFiles()?.toList() ?: emptyList()
        val currentCount = files.size
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Check Cache: Current $currentCount images, Last Update: $lastUpdate")

        try {
            when {
                currentCount < MAX_CACHE_SIZE -> {
                    val needCount = MAX_CACHE_SIZE - currentCount
                    Log.d(TAG, "Cache low, downloading $needCount images")
                    fetchAndSaveWallpapers(context, cacheDir, needCount)
                }
                currentTime - lastUpdate > UPDATE_INTERVAL_MS -> {
                    Log.d(TAG, "Cache expired, rotating 1 image")
                    fetchAndSaveWallpapers(context, cacheDir, 1)
                    removeOldestImages(cacheDir, 1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during smart update", e)
        }
    }

    /**
     * 调用 API 下载并保存壁纸
     */
    private suspend fun fetchAndSaveWallpapers(context: Context, cacheDir: File, count: Int) {
        if (count <= 0) return

        try {
            val orientation = context.resources.configuration.orientation
            val ratioParam = if (orientation == Configuration.ORIENTATION_LANDSCAPE) "gt1" else "lt1"

            Log.d(TAG, "请求 API: num=$count, ratio=$ratioParam")

            val response = api.getSetu(
                r18 = 0,
                excludeAI = true,
                num = count,
                tag = null,
                aspectRatio = ratioParam
            )

            // 只有当它 (response.error) 确实有内容时，才认为是错误
            if (!response.error.isNullOrEmpty()) {
                Log.e(TAG, "API Error: ${response.error}") // 现在这里只会打印真正的错误
                return
            }

            val dataList = response.data
            if (dataList.isNullOrEmpty()) {
                Log.e(TAG, "API 返回数据为空")
                return
            }

            Log.d(TAG, "API 返回 ${dataList.size} 条数据，开始下载...")

            var successCount = 0
            for (data in dataList) {
                val success = retry(times = 3, delayMs = 1000) {
                    saveImageToDisk(data, cacheDir)
                }
                if (success) successCount++
            }

            Log.d(TAG, "成功保存 $successCount 张壁纸")

            if (successCount > 0) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
                if (currentWallpaperUri.value == null) {
                    randomizeDisplay(context)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "下载流程异常", e)
            e.printStackTrace()
        }
    }

    /**
     * 下载单张图片到磁盘
     */
    private suspend fun saveImageToDisk(data: LoliconData, cacheDir: File): Boolean {
        val imageUrl = data.urls["regular"] ?: data.urls["original"] ?: return false
        val safePid = data.pid
        val fileName = "${safePid}_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        Log.d(TAG, "Downloading: $imageUrl")
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(imageUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                    Log.d(TAG, "Write success: ${file.name}")
                    return@withContext true
                } else {
                    Log.e(TAG, "HTTP Error: ${connection.responseCode}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download Exception: ${e.message}")
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