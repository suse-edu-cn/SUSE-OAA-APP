package com.suseoaa.projectoaa.util

import android.content.Context
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object ModelDownloader {
    private var context: Context? = null

    /**
     * 在 Android Application 或者 MainActivity 中初始化
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    actual suspend fun downloadModel(
        client: HttpClient,
        url: String,
        kaggleAuth: String?,
        onProgress: suspend (Long, Long) -> Unit
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val ctx = context ?: throw IllegalStateException("ModelDownloader not initialized with Context")
            val modelDir = File(ctx.filesDir, "ai_models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val fileName = url.substringAfterLast("/").substringBefore("?")
            val targetFile = File(modelDir, fileName)
            
            var downloadedLength = if (targetFile.exists()) targetFile.length() else 0L

            client.prepareGet(url) {
                if (!kaggleAuth.isNullOrBlank()) {
                    header("Authorization", "Basic $kaggleAuth")
                }
                if (downloadedLength > 0) {
                    header(HttpHeaders.Range, "bytes=$downloadedLength-")
                }
            }.execute { response ->
                val isPartial = response.status == HttpStatusCode.PartialContent
                
                if (!response.status.isSuccess()) {
                    if (response.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                        // 如果请求范围不符合，说明可能是之前已经下载完了，或者文件大小变化了
                        // 为了简单处理，如果是已下载完毕，其大小应该与远程一致。
                        // 如果这里返回416，通常说明文件已下完。
                        return@execute Pair(true, response.etag())
                    }
                    return@execute Pair(false, "HTTP ${response.status.value}")
                }
                
                if (!isPartial) {
                    downloadedLength = 0L // 如果服务器不支持断点续传（返回200），重置
                }
                
                val etag = response.etag()
                val contentLength = response.contentLength() ?: -1L
                val totalLength = if (contentLength != -1L) downloadedLength + contentLength else -1L

                java.io.FileOutputStream(targetFile, isPartial).use { outStream ->
                    val buffer = ByteArray(32768)
                    val channel = response.bodyAsChannel()
                    var currentRead = downloadedLength
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read > 0) {
                            outStream.write(buffer, 0, read)
                            currentRead += read
                            onProgress(currentRead, totalLength)
                        }
                    }
                    outStream.flush()
                }
                Pair(true, etag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, null)
        }
    }

    actual suspend fun getETag(
        client: HttpClient,
        url: String,
        kaggleAuth: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val response = client.head(url) {
                if (!kaggleAuth.isNullOrBlank()) {
                    header("Authorization", "Basic $kaggleAuth")
                }
            }
            if (response.status.isSuccess()) response.etag() else null
        } catch (e: Exception) {
            null
        }
    }

    actual fun isModelDownloaded(url: String): Boolean {
        val ctx = context ?: return false
        val modelDir = File(ctx.filesDir, "ai_models")
        if (!modelDir.exists()) return false
        val fileName = url.substringAfterLast("/").substringBefore("?")
        
        // 由于 CampusAiEngine 可能会为了兼容 MediaPipe 而给没有后缀的文件重命名为 .task，这里也要检查 .task 结尾的文件
        val exactFile = File(modelDir, fileName)
        val taskFile = File(modelDir, "$fileName.task")
        
        return (exactFile.exists() && exactFile.length() > 100L * 1024 * 1024) || 
               (taskFile.exists() && taskFile.length() > 100L * 1024 * 1024)
    }

    actual fun getDownloadedModels(): List<LocalModelFile> {
        val ctx = context ?: return emptyList()
        val modelDir = File(ctx.filesDir, "ai_models")
        if (!modelDir.exists()) return emptyList()
        return modelDir.listFiles()?.filter { it.isFile }?.map {
            LocalModelFile(name = it.name, sizeBytes = it.length())
        } ?: emptyList()
    }

    actual fun deleteModel(fileName: String): Boolean {
        val ctx = context ?: return false
        val modelDir = File(ctx.filesDir, "ai_models")
        val targetFile = File(modelDir, fileName)
        return if (targetFile.exists()) {
            targetFile.delete()
        } else {
            false
        }
    }
}
