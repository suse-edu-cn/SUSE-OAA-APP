package com.suseoaa.projectoaa.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS 平台的图片处理工具实现
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object PlatformImageUtils {
    
    /**
     * 将 Base64 字符串解码为 ImageBitmap
     */
    actual fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap? {
        return try {
            // 处理 data URI 格式
            val base64Data = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            // 使用 NSData 解码 Base64
            val nsData = NSData.create(
                base64EncodedString = base64Data,
                options = NSDataBase64DecodingIgnoreUnknownCharacters
            ) ?: return null
            
            // 转换为字节数组
            val bytes = nsData.toByteArray()
            if (bytes.isEmpty()) return null
            
            decodeByteArrayToImageBitmap(bytes)
        } catch (e: Exception) {
            println("[PlatformImageUtils] Base64解码失败: ${e.message}")
            null
        }
    }
    
    /**
     * 将字节数组解码为 ImageBitmap
     */
    actual fun decodeByteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            // 使用 Skia 解码图片
            val skiaImage = Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("[PlatformImageUtils] 字节数组解码失败: ${e.message}")
            null
        }
    }
    
    /**
     * NSData 转 ByteArray
     */
    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned: Pinned<ByteArray> ->
                memcpy(pinned.addressOf(0), this.bytes, length.toULong())
            }
        }
        return bytes
    }
}
