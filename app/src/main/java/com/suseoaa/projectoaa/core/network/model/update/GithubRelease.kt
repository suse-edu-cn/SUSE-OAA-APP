package com.suseoaa.projectoaa.core.network.model.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String, // 版本号，如 "v1.0.1"
    @SerialName("body")
    val body: String,    // 更新日志
    @SerialName("assets")
    val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    @SerialName("browser_download_url")
    val downloadUrl: String, // APK 下载链接
    @SerialName("name")
    val name: String         // 文件名
)
