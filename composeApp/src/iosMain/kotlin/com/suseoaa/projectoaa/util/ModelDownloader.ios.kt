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
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileHandle
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.createFileAtPath
import platform.Foundation.fileHandleForWritingAtPath

actual object ModelDownloader {
    
    @OptIn(ExperimentalForeignApi::class)
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
            
            if (fileManager.fileExistsAtPath(targetFilePath)) {
                fileManager.removeItemAtPath(targetFilePath, null)
            }
            
            fileManager.createFileAtPath(targetFilePath, null, null)
            val fileHandle = NSFileHandle.fileHandleForWritingAtPath(targetFilePath)
            
            if (fileHandle == null) return@withContext Pair(false, null)

            client.prepareGet(url) {
                if (!kaggleAuth.isNullOrBlank()) {
                    header("Authorization", "Basic $kaggleAuth")
                }
                onDownload { bytesSentTotal, contentLength ->
                    onProgress(bytesSentTotal, contentLength ?: -1L)
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    fileHandle.closeFile()
                    return@execute Pair(false, "HTTP ${response.status.value}")
                }
                val etag = response.etag()
                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = ByteArray(8192)
                
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read > 0) {
                        val nsData = buffer.usePinned { pinned ->
                            NSData.create(
                                bytes = pinned.addressOf(0),
                                length = read.toULong()
                            )
                        }
                        fileHandle.writeData(nsData)
                    }
                }
                fileHandle.closeFile()
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
        val fileSize = attributes?.get(platform.Foundation.NSFileSize) as? Long ?: 0L
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
            val fileSize = attributes?.get(platform.Foundation.NSFileSize) as? Long ?: 0L
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
