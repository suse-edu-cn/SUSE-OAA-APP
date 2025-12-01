package com.suseoaa.projectoaa.common.util

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.suseoaa.projectoaa.common.network.LoliconApi
import com.suseoaa.projectoaa.common.network.LoliconData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 壁纸管理单例对象
 * 负责壁纸的缓存管理、网络下载、轮播显示以及每日打卡图的获取。
 */
object WallpaperManager {

    // ==========================================
    // 1. 常量配置
    // ==========================================
    private const val TAG = "WallpaperManager"
    private const val PREF_NAME = "wallpaper_prefs"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val KEY_WALLPAPER_ALPHA = "key_wallpaper_alpha_value"

    private const val CACHE_DIR_NAME = "anime_wallpapers"
    private const val CHECK_IN_FILE_NAME = "daily_check_in_cover.jpg"
    private const val TEMP_SUFFIX = ".tmp"

    private const val MAX_CACHE_SIZE = 5
    private const val MIN_IMAGE_SIZE = 20 * 1024L // 20KB
    private const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6小时
    private const val EASTER_EGG_TRIGGER_COUNT = 7 // 连续点击触发重置次数
    private const val DEFAULT_ALPHA = 0.85f

    // [优化] 提取常量，用于 isSameDay
    private const val DAY_IN_MS = 1000 * 60 * 60 * 24

    // ==========================================
    // 2. 状态管理
    // ==========================================
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val api by lazy { LoliconApi.create() }

    // [优化] 使用 AtomicBoolean 替代 @Volatile var
    private val isInitializing = AtomicBoolean(false)
    // [优化] 使用 AtomicInteger 替代 var
    private val refreshClickCount = AtomicInteger(0)

    // 壁纸 URI 流
    private val _currentWallpaper = MutableStateFlow<Uri?>(null)
    val currentWallpaper = _currentWallpaper.asStateFlow()

    // 透明度流
    private val _wallpaperAlpha = MutableStateFlow(DEFAULT_ALPHA)
    val wallpaperAlpha = _wallpaperAlpha.asStateFlow()

    // ==========================================
    // 3. 初始化与公共API
    // ==========================================

    /**
     * App 启动时初始化
     */
    fun initialize(context: Context) {
        // [优化] 使用 AtomicBoolean.getAndSet 确保线程安全
        if (isInitializing.getAndSet(true)) return

        loadWallpaperAlpha(context)

        scope.launch { // <--- 协程从这里开始
            try {
                // 1. [优化] (后台) 立即随机展示一张本地缓存
                // 将 I/O 操作从主线程移到后台
                randomizeDisplay(context)

                // 2. [数据] 后台静默更新：下载新图，删除旧图
                smartUpdateCache(context, forceNow = true)
                // 3. [打卡] 预加载今日打卡图
                getCheckInImage(context)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization error", e)
            } finally {
                isInitializing.set(false)
            }
        }
    }

    /**
     * 设置并保存透明度
     */
    fun setWallpaperAlpha(context: Context, alpha: Float) {
        _wallpaperAlpha.value = alpha
        scope.launch {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_WALLPAPER_ALPHA, alpha).apply()
        }
    }

    /**
     * 用户点击刷新壁纸
     */
    fun refreshWallpaper(context: Context) {
        val clickCount = refreshClickCount.incrementAndGet()

        scope.launch { // <--- 协程从这里开始
            // 1. 尝试切换到下一张本地图片 (I/O)
            val nextFile = randomizeDisplay(context) // <--- 移动到协程内部，错误消失

            // 2. 判断缓存状态
            val cacheDir = getCacheDir(context)
            val validFilesCount = getValidImageFiles(cacheDir).size
            val isEasterEggTriggered = clickCount >= EASTER_EGG_TRIGGER_COUNT

            // 3. 触发下载条件
            val needDownload = (nextFile == null) || (validFilesCount < 2) || isEasterEggTriggered

            // 4. 显示 Toast
            withContext(Dispatchers.Main) {
                val msg = if (needDownload) "正在获取新壁纸..." else "已切换壁纸"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }

            // 5. 执行后续的下载或清理
            if (isEasterEggTriggered) {
                Log.i(TAG, "Easter egg triggered: Force cleaning cache.")
                refreshClickCount.set(0) // 重置计数器
                forceCleanAndUpdate(context, keepFile = nextFile)
            } else {
                smartUpdateCache(context, forceNow = needDownload)
            }
        }
    }

    /**
     * 保存当前壁纸到系统相册
     */
    fun saveCurrentToGallery(context: Context) {
        val currentUri = _currentWallpaper.value
        if (currentUri?.path == null) {
            Toast.makeText(context, "当前没有壁纸", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val sourceFile = File(currentUri.path!!)
            if (!sourceFile.exists()) return@launch

            val fileName = "OAA_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectOAA")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            try {
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input -> input.copyTo(output) }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save to gallery failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // 4. 每日打卡图逻辑 (Strategies)
    // ==========================================

    /**
     * 获取每日打卡图
     * 策略：缓存优先 -> 网络(1:1) -> 网络(任意) -> 本地壁纸库 -> 过期缓存兜底
     */
    suspend fun getCheckInImage(context: Context): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir(context)
        val finalFile = File(cacheDir, CHECK_IN_FILE_NAME)

        // --- 策略 1: 缓存优先 ---
        // [优化] isSameDay 已被优化，isValidImage 是I/O操作，但已在IO线程
        if (finalFile.isValidImage() && isSameDay(finalFile.lastModified(), System.currentTimeMillis())) {
            Log.d(TAG, "getCheckInImage: [HIT] Cache valid.")
            return@withContext Uri.fromFile(finalFile)
        }

        Log.d(TAG, "getCheckInImage: [MISSING/EXPIRED] Starting fetch...")

        // --- 策略 2 & 3: 网络下载 (优先正方形，失败则任意) ---
        var newUri = tryDownloadCheckInImage(cacheDir, finalFile, aspectRatio = "eq1")
        if (newUri == null) {
            Log.w(TAG, "getCheckInImage: Strict (eq1) failed, trying relaxed mode.")
            newUri = tryDownloadCheckInImage(cacheDir, finalFile, aspectRatio = null)
        }

        // --- 策略 4: 本地借用 (偷一张普通壁纸) ---
        if (newUri == null) {
            Log.w(TAG, "getCheckInImage: Network failed, trying local fallback.")
            // 此时不调用 randomizeDisplay 以免影响 UI，而是直接读文件 (I/O)
            val randomWallpaper = getValidImageFiles(cacheDir).randomOrNull()
            if (randomWallpaper != null) {
                try {
                    randomWallpaper.copyTo(finalFile, overwrite = true)
                    finalFile.setLastModified(System.currentTimeMillis()) // 更新时间戳
                    newUri = Uri.fromFile(finalFile)
                } catch (e: Exception) {
                    Log.e(TAG, "getCheckInImage: Fallback copy failed", e)
                }
            }
        }

        // --- 策略 5: 烂缓存兜底 (实在没办法了，昨天的图也能用) ---
        if (newUri == null && finalFile.isValidImage()) {
            Log.w(TAG, "getCheckInImage: All failed. Returning EXPIRED cache.")
            return@withContext Uri.fromFile(finalFile)
        }

        return@withContext newUri
    }

    // ==========================================
    // 5. 内部私有方法
    // ==========================================

    private fun loadWallpaperAlpha(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _wallpaperAlpha.value = prefs.getFloat(KEY_WALLPAPER_ALPHA, DEFAULT_ALPHA)
    }

    /**
     * 从本地随机选取一张壁纸显示
     * [优化] 这是一个 suspend 函数，因为它执行 I/O 操作。
     */
    private suspend fun randomizeDisplay(context: Context): File? = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir(context)
        val files = getValidImageFiles(cacheDir) // I/O 操作

        if (files.isNotEmpty()) {
            var randomFile = files.random()
            // 如果只有一张图，就没得选；如果有通过多张，尽量选和当前不一样的一张
            if (files.size > 1 && _currentWallpaper.value?.path == randomFile.path) {
                randomFile = files.filter { it.path != randomFile.path }.random()
            }

            _currentWallpaper.value = Uri.fromFile(randomFile)
            return@withContext randomFile
        }
        return@withContext null
    }

    /**
     * 智能更新缓存
     */
    private suspend fun smartUpdateCache(context: Context, forceNow: Boolean) = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir(context)
        cleanupTempFiles(cacheDir)

        val validFiles = getValidImageFiles(cacheDir).sortedBy { it.lastModified() }
        val currentCount = validFiles.size

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()
        val isExpired = currentTime - lastUpdate > UPDATE_INTERVAL_MS

        try {
            if (currentCount < MAX_CACHE_SIZE) {
                // 场景A: 缓存不足，补齐
                val needCount = MAX_CACHE_SIZE - currentCount
                fetchAndSaveParallel(context, cacheDir, needCount)
            } else if (isExpired || forceNow) {
                // 场景B: 缓存已满但过期/强制刷新 -> 下载1张新图，成功后删掉1张旧图 (滚动更新)
                val successCount = fetchAndSaveParallel(context, cacheDir, 1)
                if (successCount > 0 && validFiles.isNotEmpty()) {
                    validFiles.first().delete() // 删除最旧的一张
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SmartUpdate failed", e)
        }
    }

    /**
     * 强制清空并重新下载 (彩蛋逻辑)
     */
    private suspend fun forceCleanAndUpdate(context: Context, keepFile: File?) = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir(context)
        cacheDir.listFiles()?.forEach { file ->
            if (file.name != CHECK_IN_FILE_NAME && (keepFile == null || file.absolutePath != keepFile.absolutePath)) {
                file.delete()
            }
        }
        fetchAndSaveParallel(context, cacheDir, MAX_CACHE_SIZE)
    }

    private suspend fun fetchAndSaveParallel(context: Context, cacheDir: File, count: Int): Int {
        if (count <= 0) return 0

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val ratioParam = if (isLandscape) "gt1" else "lt1" // 横屏要宽图，竖屏要长图

        return try {
            // 优先尝试获取符合屏幕比例的图片
            var response = api.getSetu(r18 = 0, excludeAI = true, num = count, aspectRatio = ratioParam)
            if (response.data.isNullOrEmpty()) {
                // 降级：不限制比例
                response = api.getSetu(r18 = 0, excludeAI = true, num = count, aspectRatio = null)
            }

            val dataList = response.data ?: return 0

            // 并发下载
            supervisorScope {
                val tasks = dataList.map { data ->
                    async {
                        // 简单的重试机制
                        retry(times = 2, delayMs = 1000) {
                            processSingleImageDownload(data, cacheDir)
                        }
                    }
                }
                val results = tasks.awaitAll()
                val successCount = results.count { it }

                if (successCount > 0) {
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
                    // 如果当前没壁纸，下载完立刻显示
                    if (_currentWallpaper.value == null) {
                        randomizeDisplay(context)
                    }
                }
                successCount
            }
        } catch (e: Exception) {
            Log.e(TAG, "FetchAndSave failed", e)
            0
        }
    }

    private suspend fun processSingleImageDownload(data: LoliconData, cacheDir: File): Boolean {
        val urlCandidates = listOfNotNull(data.urls["regular"], data.urls["original"], data.urls["small"])
        if (urlCandidates.isEmpty()) return false

        val fileName = "${data.pid}_${System.currentTimeMillis()}.jpg"
        val finalFile = File(cacheDir, fileName)
        val tempFile = File(cacheDir, fileName + TEMP_SUFFIX)

        // 遍历 URL 列表尝试下载，只要有一个成功即可
        for (url in urlCandidates) {
            if (downloadUrlToFile(url, tempFile)) {
                if (tempFile.renameTo(finalFile)) return true
                tempFile.delete()
            }
        }
        return false
    }

    private suspend fun tryDownloadCheckInImage(cacheDir: File, destination: File, aspectRatio: String?): Uri? {
        return try {
            val response = api.getSetu(r18 = 0, excludeAI = true, num = 1, aspectRatio = aspectRatio)
            if (!response.error.isNullOrEmpty()) return null

            val data = response.data?.firstOrNull() ?: return null
            val urlCandidates = listOfNotNull(data.urls["regular"], data.urls["original"], data.urls["small"])

            val tempFile = File(cacheDir, "check_in_temp$TEMP_SUFFIX")

            var success = false
            for (url in urlCandidates) {
                if (downloadUrlToFile(url, tempFile)) {
                    success = true
                    break
                }
            }

            if (success) {
                if (destination.exists()) destination.delete()
                if (tempFile.renameTo(destination)) {
                    Uri.fromFile(destination)
                } else {
                    tempFile.delete()
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "tryDownloadCheckInImage failed", e)
            null
        }
    }

    /**
     * 底层网络下载实现
     */
    private suspend fun downloadUrlToFile(urlString: String, destination: File): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false

            connection.inputStream.use { input ->
                BufferedInputStream(input).use { bufferedInput ->
                    FileOutputStream(destination).use { output ->
                        bufferedInput.copyTo(output)
                    }
                }
            }

            // 校验文件大小，避免下载到损坏的空文件
            if (destination.exists() && destination.length() > MIN_IMAGE_SIZE) {
                return@withContext true
            } else {
                destination.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            if (destination.exists()) destination.delete()
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    // ==========================================
    // 6. 辅助工具
    // ==========================================

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // [优化] 此函数仍然执行 I/O，但它现在只在 IO 线程上被调用
    private fun getValidImageFiles(dir: File): List<File> {
        return dir.listFiles { _, name ->
            name.endsWith(".jpg") && name != CHECK_IN_FILE_NAME
        }?.filter { it.length() > MIN_IMAGE_SIZE }?.toList() ?: emptyList()
    }

    private fun cleanupTempFiles(dir: File) {
        try {
            dir.listFiles { _, name -> name.endsWith(TEMP_SUFFIX) }?.forEach { it.delete() }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun File.isValidImage(): Boolean = this.exists() && this.length() > MIN_IMAGE_SIZE

    /**
     * [优化] 高效的日期比较，避免使用 Calendar.getInstance()
     */
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        // 必须考虑时区，否则0点附近会出错
        val timeZoneOffset = TimeZone.getDefault().getOffset(time1)
        val day1 = (time1 + timeZoneOffset) / DAY_IN_MS
        val day2 = (time2 + timeZoneOffset) / DAY_IN_MS
        return day1 == day2
    }

    private suspend fun retry(times: Int, delayMs: Long, block: suspend () -> Boolean): Boolean {
        repeat(times) {
            if (block()) return true
            delay(delayMs)
        }
        return false
    }
}