package com.suseoaa.projectoaa.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Android 平台的图片处理工具实现
 */
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
            
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
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
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            println("[PlatformImageUtils] 字节数组解码失败: ${e.message}")
            null
        }
    }
}
