package com.suseoaa.projectoaa.util

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual object ModelDownloader {
    
    actual suspend fun downloadModel(
        client: HttpClient,
        url: String,
        kaggleAuth: String?,
        onProgress: suspend (Long, Long) -> Unit
    ): Pair<Boolean, String?> = withContext(Dispatchers.Default) {
        try {
            val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            val documentsDirectory = paths.first() as String
            val modelDir = "$documentsDirectory/ai_models"
            
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(modelDir)) {
                fileManager.createDirectoryAtPath(modelDir, true, null, null)
            }
            
            val fileName = url.substringAfterLast("/").substringBefore("?")
            val targetFilePath = "$modelDir/$fileName"
            
            var downloadedLength = 0L
            if (fileManager.fileExistsAtPath(targetFilePath)) {
                val attributes = fileManager.attributesOfItemAtPath(targetFilePath, null)
                downloadedLength = (attributes?.get(platform.Foundation.NSFileSize) as? platform.Foundation.NSNumber)?.longValue ?: 0L
            }
            
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
                        return@execute Pair(true, response.etag())
                    }
                    return@execute Pair(false, "HTTP ${response.status.value}")
                }
                
                val fileMode = if (isPartial) "ab" else "wb"
                if (!isPartial) {
                    downloadedLength = 0L
                }
                
                val file = fopen(targetFilePath, fileMode)
                if (file == null) {
                    return@execute Pair(false, "Cannot open file for writing")
                }
                
                val etag = response.etag()
                val contentLength = response.contentLength() ?: -1L
                val totalLength = if (contentLength != -1L) downloadedLength + contentLength else -1L

                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = ByteArray(32768)
                var currentRead = downloadedLength
                
                try {
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read > 0) {
                            buffer.usePinned { pinned ->
                                fwrite(pinned.addressOf(0), 1u, read.toULong(), file)
                            }
                            currentRead += read
                            onProgress(currentRead, totalLength)
                        }
                    }
                } finally {
                    fclose(file)
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
    ): String? = withContext(Dispatchers.Default) {
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
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.first() as String
        val modelDir = "$documentsDirectory/ai_models"
        val fileName = url.substringAfterLast("/").substringBefore("?")
        val targetFilePath = "$modelDir/$fileName"
        
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(targetFilePath)) return false
        
        // Ensure file is not empty
        val attributes = fileManager.attributesOfItemAtPath(targetFilePath, null)
        val fileSize = (attributes?.get(platform.Foundation.NSFileSize) as? platform.Foundation.NSNumber)?.longValue ?: 0L
        return fileSize > 0L
    }

    actual fun getDownloadedModels(): List<LocalModelFile> {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.first() as String
        val modelDir = "$documentsDirectory/ai_models"
        
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(modelDir)) return emptyList()
        
        val contents = fileManager.contentsOfDirectoryAtPath(modelDir, null) as? List<String> ?: return emptyList()
        
        return contents.mapNotNull { fileName ->
            val filePath = "$modelDir/$fileName"
            val attributes = fileManager.attributesOfItemAtPath(filePath, null)
            val fileSize = (attributes?.get(platform.Foundation.NSFileSize) as? platform.Foundation.NSNumber)?.longValue ?: 0L
            if (fileSize > 0) LocalModelFile(fileName, fileSize) else null
        }
    }

    actual fun deleteModel(fileName: String): Boolean {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.first() as String
        val modelDir = "$documentsDirectory/ai_models"
        val targetFilePath = "$modelDir/$fileName"
        
        val fileManager = NSFileManager.defaultManager
        return if (fileManager.fileExistsAtPath(targetFilePath)) {
            fileManager.removeItemAtPath(targetFilePath, null)
        } else {
            false
        }
    }
}
