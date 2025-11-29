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
    // 当前显示的壁纸 Uri
    var currentWallpaperUri = mutableStateOf<Uri?>(null)
        private set

    private const val TAG = "WallpaperManager"
    private const val PREF_NAME = "wallpaper_prefs"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val CACHE_DIR_NAME = "anime_wallpapers"
    private const val MAX_CACHE_SIZE = 5
    // 更新间隔 (这里设为 12 小时，调试时可忽略)
    private const val UPDATE_INTERVAL_MS = 12 * 60 * 60 * 1000L

    private val api = LoliconApi.create()
    // 使用 IO 调度器执行后台任务
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 初始化：随机显示一张已有壁纸，并检查是否需要下载新壁纸
     */
    fun initialize(context: Context) {
        randomizeDisplay(context)
        scope.launch { smartUpdateCache(context) }
    }

    /**
     * 随机切换当前显示的壁纸
     */
    fun randomizeDisplay(context: Context) {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (cacheDir.exists()) {
            val files = cacheDir.listFiles()?.toList() ?: emptyList()
            if (files.isNotEmpty()) {
                val randomFile = files.random()
                // 切换到主线程更新 UI 状态
                scope.launch(Dispatchers.Main) {
                    // 尝试不重复显示同一张
                    if (currentWallpaperUri.value?.path == randomFile.path && files.size > 1) {
                        val otherFiles = files.filter { it.path != randomFile.path }
                        if (otherFiles.isNotEmpty()) {
                            currentWallpaperUri.value = Uri.fromFile(otherFiles.random())
                            return@launch
                        }
                    }
                    currentWallpaperUri.value = Uri.fromFile(randomFile)
                    Log.d(TAG, "已切换壁纸: ${randomFile.name}")
                }
            } else {
                Log.d(TAG, "缓存目录为空，无法显示壁纸")
            }
        }
    }

    /**
     * 保存当前壁纸到系统相册
     */
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
                // 1. 准备保存参数
                val fileName = "OAA_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    // Android Q (10) 以上使用 Scoped Storage
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectOAA")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                // 2. 插入记录并写入数据
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    // 3. 标记写入完成
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
                // 处理权限异常
                handleSaveException(context, e)
            }
        }
    }

    private suspend fun handleSaveException(context: Context, e: Exception) {
        if (e is SecurityException && context is Activity) {
            withContext(Dispatchers.Main) {
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
                } else {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1002
                    )
                    Toast.makeText(context, "请授权存储权限后重试", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                val msg = if (e is SecurityException) "保存失败：缺少权限" else "保存失败: ${e.message}"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 智能更新缓存：数量不足或时间过期时下载
     */
    private suspend fun smartUpdateCache(context: Context) {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val files = cacheDir.listFiles()?.toList() ?: emptyList()
        val currentCount = files.size
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "检查缓存: 当前 $currentCount 张, 上次更新: $lastUpdate")

        try {
            when {
                // 1. 数量不足，立即补充
                currentCount < MAX_CACHE_SIZE -> {
                    val needCount = MAX_CACHE_SIZE - currentCount
                    Log.d(TAG, "缓存不足，准备下载 $needCount 张")
                    fetchAndSaveWallpapers(context, cacheDir, needCount)
                }
                // 2. 时间过期，轮换 1 张
                currentTime - lastUpdate > UPDATE_INTERVAL_MS -> {
                    Log.d(TAG, "缓存已过期，轮换 1 张")
                    fetchAndSaveWallpapers(context, cacheDir, 1)
                    removeOldestImages(cacheDir, 1)
                }
                else -> {
                    Log.d(TAG, "缓存充足且未过期，无需下载")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新缓存时发生错误", e)
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

            // [核心修改] 移除了 tag 限制，提高匹配率
            val response = api.getSetu(
                r18 = 0,
                excludeAI = true,
                num = count,
                tag = null, // 设置为 null，获取随机二次元图
                aspectRatio = ratioParam
            )

            // 检查 API 级错误
            if (response.error != null) {
                Log.e(TAG, "API Error: ${response.error}")
                return
            }

            val dataList = response.data
            if (dataList.isNullOrEmpty()) {
                Log.e(TAG, "API 返回数据为空 (可能条件过于严格)")
                return
            }

            Log.d(TAG, "API 返回 ${dataList.size} 条数据，开始下载...")

            var successCount = 0
            for (data in dataList) {
                // 简单的重试机制
                val success = retry(times = 3, delayMs = 1000) {
                    saveImageToDisk(data, cacheDir)
                }
                if (success) successCount++
            }

            Log.d(TAG, "成功保存 $successCount 张壁纸")

            if (successCount > 0) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()

                // 如果当前界面还没显示壁纸，立即刷新
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
     * 下载单张图片并写入磁盘
     */
    private suspend fun saveImageToDisk(data: LoliconData, cacheDir: File): Boolean {
        // 优先尝试 regular 尺寸，没有则用 original
        val imageUrl = data.urls["regular"] ?: data.urls["original"] ?: return false

        // 确保 pid 和 title 不为空，防止空指针（虽然 API 定义已改为可空）
        val safePid = data.pid
        val fileName = "${safePid}_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)

        Log.d(TAG, "正在下载: $imageUrl")

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
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "写入成功: ${file.name}")
                    return@withContext true
                } else {
                    Log.e(TAG, "HTTP 错误: ${connection.responseCode}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载异常: ${e.message}")
                if (file.exists()) file.delete() // 下载失败删除半成品
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