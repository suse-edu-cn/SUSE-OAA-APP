package com.suseoaa.projectoaa.util

import io.ktor.client.*

/**
 * 跨平台的模型下载工具
 * 使用 Ktor HttpClient 进行网络请求和进度捕获，使用底层系统 API 进行文件存储
 */
expect object ModelDownloader {
    /**
     * 下载模型文件到应用私有目录
     * @param client Ktor HttpClient 实例
     * @param url 下载链接
     * @param onProgress 进度回调 (已下载字节, 总字节)
     * @return 下载成功与否
     */
    suspend fun downloadModel(
        client: HttpClient,
        url: String,
        kaggleAuth: String? = null,
        onProgress: suspend (Long, Long) -> Unit
    ): Pair<Boolean, String?>

    /**
     * 轻量级获取远程模型的 ETag（用于检测更新）
     */
    suspend fun getETag(
        client: HttpClient,
        url: String,
        kaggleAuth: String? = null
    ): String?

    /**
     * 检测本地是否已成功下载该模型文件
     */
    fun isModelDownloaded(url: String): Boolean

    /**
     * 获取所有已下载的本地模型文件列表
     */
    fun getDownloadedModels(): List<LocalModelFile>

    /**
     * 删除指定的本地模型文件
     * @param fileName 模型文件名
     * @return 删除是否成功
     */
    fun deleteModel(fileName: String): Boolean
}
