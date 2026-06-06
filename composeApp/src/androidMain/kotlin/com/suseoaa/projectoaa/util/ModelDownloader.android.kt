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
            
            client.prepareGet(url) {
                if (!kaggleAuth.isNullOrBlank()) {
                    header("Authorization", "Basic $kaggleAuth")
                }
                onDownload { bytesSentTotal, contentLength ->
                    onProgress(bytesSentTotal, contentLength ?: -1L)
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    return@execute Pair(false, "HTTP ${response.status.value}")
                }
                val etag = response.etag()

                targetFile.outputStream().use { outStream ->
                    val buffer = ByteArray(8192)
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read > 0) {
                            outStream.write(buffer, 0, read)
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
