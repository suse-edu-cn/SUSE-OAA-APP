package com.suseoaa.projectoaa.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 平台相关的图片处理工具
 */
expect object PlatformImageUtils {
    /**
     * 将 Base64 字符串解码为 ImageBitmap
     * @param base64String Base64 编码的图片数据
     * @return ImageBitmap 或 null
     */
    fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap?
    
    /**
     * 将字节数组解码为 ImageBitmap
     * @param bytes 图片字节数组
     * @return ImageBitmap 或 null
     */
    fun decodeByteArrayToImageBitmap(bytes: ByteArray): ImageBitmap?
}
