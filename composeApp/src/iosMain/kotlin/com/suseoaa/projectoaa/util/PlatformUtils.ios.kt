package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.UIKit.*
import platform.PhotosUI.*
import platform.CoreGraphics.*
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun pickImageForAvatar(onImagePicked: (ByteArray?) -> Unit) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val config = PHPickerConfiguration()
        config.selectionLimit = 1
        config.filter = PHPickerFilter.imagesFilter

        val picker = PHPickerViewController(configuration = config)

        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(true, completion = null)

                val results = didFinishPicking as? List<PHPickerResult>
                val result = results?.firstOrNull() ?: run {
                    scope.launch { onImagePicked(null) }
                    return
                }

                result.itemProvider.loadDataRepresentationForTypeIdentifier(
                    typeIdentifier = "public.image"
                ) { data, error ->
                    if (error != null || data == null) {
                        scope.launch { onImagePicked(null) }
                        return@loadDataRepresentationForTypeIdentifier
                    }

                    // 压缩图片
                    val imageData = compressImage(data)
                    scope.launch { onImagePicked(imageData) }
                }
            }
        }

        picker.delegate = delegate

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }
}


@OptIn(ExperimentalForeignApi::class)
private fun compressImage(
    data: NSData,
    maxWidth: Double = 800.0,
    maxHeight: Double = 800.0,
    quality: Double = 0.8
): ByteArray? {
    return try {
        val image = UIImage.imageWithData(data) ?: return null

        // 计算缩放比例
        val width = image.size.useContents { this.width }
        val height = image.size.useContents { this.height }
        val scale = minOf(
            maxWidth / width,
            maxHeight / height,
            1.0
        )

        val newWidth = width * scale
        val newHeight = height * scale

        // 创建缩放后的图片
        memScoped {
            val newSize = alloc<CGSize> {
                this.width = newWidth
                this.height = newHeight
            }
            UIGraphicsBeginImageContextWithOptions(newSize.readValue(), false, 1.0)

            val rect = alloc<CGRect> {
                origin.x = 0.0
                origin.y = 0.0
                size.width = newWidth
                size.height = newHeight
            }
            image.drawInRect(rect.readValue())
            val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            // 压缩为JPEG
            val compressedData = scaledImage?.let { UIImageJPEGRepresentation(it, quality) }

            // 转换为ByteArray
            compressedData?.let { nsData ->
                ByteArray(nsData.length.toInt()).apply {
                    usePinned {
                        memcpy(it.addressOf(0), nsData.bytes, nsData.length)
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Image compression error: ${e.message}")
        null
    }
}
