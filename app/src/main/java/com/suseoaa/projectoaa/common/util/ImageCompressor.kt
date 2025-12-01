package com.suseoaa.projectoaa.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext // 1. 导入
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject     // 2. 导入
import javax.inject.Singleton // 3. 导入

@Singleton // 4. 声明为单例
class ImageCompressor @Inject constructor( // 5. 改为 class, 并使用 @Inject 构造函数
    @ApplicationContext private val context: Context // 6. 在这里注入 Context
) {

    private val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    private val MAX_IMAGE_DIMENSION = 2048 // 限制最大边长，防止大图解码OOM

    /**
     * 压缩图片到指定文件大小（最大5MB），并返回压缩后的临时文件 Uri。
     * 如果图片本身小于5MB，会返回原始 Uri。
     */
    // 7. 移除 context 参数
    suspend fun compressImage(imageUri: Uri?): Uri? =
        withContext(Dispatchers.IO) {
            if (imageUri == null) return@withContext null

            try {
                // 8. 使用类成员 'context'
                val originalSize =
                    context.contentResolver.openInputStream(imageUri)?.use { it.available() } ?: 0
                if (originalSize <= MAX_FILE_SIZE_BYTES) {
                    return@withContext imageUri // 小于 5MB，直接返回原 Uri
                }

                // 2. 缩放解码，防止OOM
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                // 8. 使用类成员 'context'
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                var scale = 1
                while (options.outWidth / scale > MAX_IMAGE_DIMENSION || options.outHeight / scale > MAX_IMAGE_DIMENSION) {
                    scale *= 2
                }
                options.inSampleSize = scale
                options.inJustDecodeBounds = false

                var bitmap: Bitmap? = null
                // 8. 使用类成员 'context'
                context.contentResolver.openInputStream(imageUri)?.use {
                    bitmap = BitmapFactory.decodeStream(it, null, options)
                }

                // 3. 旋转矫正 (根据 Exif 信息)
                val rotation = getImageRotation(imageUri) // 9. 调用修改后的私有方法
                if (rotation != 0 && bitmap != null) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap!!,
                        0,
                        0,
                        bitmap!!.width,
                        bitmap!!.height,
                        matrix,
                        true
                    )
                    bitmap!!.recycle() // 回收旧的
                    bitmap = rotatedBitmap
                }

                if (bitmap == null) return@withContext null

                // 4. 质量压缩 (迭代降低质量直到满足大小要求)
                var compressQuality = 90
                var outputStream = ByteArrayOutputStream()
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)

                while (outputStream.toByteArray().size > MAX_FILE_SIZE_BYTES && compressQuality > 10) {
                    outputStream.reset()
                    compressQuality -= 10
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
                }

                // 5. 保存到临时文件
                val tempFile =
                    // 8. 使用类成员 'context'
                    File(context.cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(outputStream.toByteArray())
                    fos.flush()
                }
                outputStream.close()
                bitmap?.recycle()

                return@withContext Uri.fromFile(tempFile)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    // 10. 移除 context 参数
    private fun getImageRotation(uri: Uri): Int {
        var rotation = 0
        // 11. 使用类成员 'context'
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exifInterface = ExifInterface(inputStream)
            when (exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
            }
        }
        return rotation
    }

    /**
     * [新增] 安全地清理压缩过的临时文件
     * 这个函数只删除位于 cacheDir 中的文件，不会误删 content:// 路径
     */
    fun cleanupCompressedImage(uri: Uri) {
        try {
            // 只清理我们自己创建的 (file:// 协议 且 路径在 cacheDir 内)
            if (uri.scheme == "file" && uri.path?.startsWith(context.cacheDir.path) == true) {
                uri.path?.let {
                    val file = File(it)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // 记录日志，但不使应用崩溃
            e.printStackTrace()
        }
    }
}