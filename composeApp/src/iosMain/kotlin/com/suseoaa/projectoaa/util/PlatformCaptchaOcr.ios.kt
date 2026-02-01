package com.suseoaa.projectoaa.util

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS平台的OCR验证码识别实现
 * 优先尝试调用 Swift 的 CaptchaOCR（内部使用 ddddocr），
 * 如果不可用则回退到 Kotlin/Native 直接调用 Vision Framework
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object PlatformCaptchaOcr {
    
    /**
     * 识别验证码图片中的文字
     */
    actual suspend fun recognize(imageBytes: ByteArray): Result<String> {
        return suspendCoroutine { continuation ->
            try {
                // 将字节数组转换为 NSData
                val nsData = imageBytes.toNSData()
                
                // 尝试使用 Swift CaptchaOCR（如果可用）
                // 注意：由于 Kotlin/Native 无法直接调用 Swift 类，
                // 我们继续使用 Vision Framework
                // 如果需要使用 ddddocr，请在 Swift 侧通过桥接实现
                
                // 创建 UIImage
                val uiImage = UIImage.imageWithData(nsData)
                if (uiImage == null) {
                    continuation.resume(Result.failure(Exception("无法解码图片")))
                    return@suspendCoroutine
                }
                
                // 获取 CGImage
                val cgImage = uiImage.CGImage
                if (cgImage == null) {
                    continuation.resume(Result.failure(Exception("无法获取CGImage")))
                    return@suspendCoroutine
                }
                
                // 创建文字识别请求
                var recognizedText = ""
                
                val request = VNRecognizeTextRequest { request, error ->
                    if (error != null) {
                        println("[iOS OCR] 识别错误: ${error.localizedDescription}")
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val observations = request?.results as? List<VNRecognizedTextObservation>
                        observations?.forEach { observation ->
                            val candidates = observation.topCandidates(1u)
                            candidates.firstOrNull()?.let { candidate ->
                                // VNRecognizedText 的 string 属性
                                @Suppress("UNCHECKED_CAST")
                                val text = (candidate as? Any)?.toString() ?: ""
                                if (text.isNotEmpty() && !text.startsWith("<")) {
                                    recognizedText += text
                                }
                            }
                        }
                    }
                }
                
                // 配置识别参数 - 增加更多设置以提高验证码识别率
                request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
                request.setRecognitionLanguages(listOf("en-US"))
                request.usesLanguageCorrection = false
                request.setMinimumTextHeight(0.0f) // 识别更小的文字
                
                // 执行识别
                val handler = VNImageRequestHandler(cgImage, emptyMap<Any?, Any?>())
                val success = handler.performRequests(listOf(request), null)
                
                if (!success) {
                    continuation.resume(Result.failure(Exception("识别请求执行失败")))
                    return@suspendCoroutine
                }
                
                // 清理识别结果
                val cleanedText = cleanCaptchaText(recognizedText)
                
                if (cleanedText.isBlank()) {
                    continuation.resume(Result.failure(Exception("未能识别出验证码")))
                } else {
                    println("[iOS OCR] 识别结果: $cleanedText (原始: $recognizedText)")
                    continuation.resume(Result.success(cleanedText))
                }
            } catch (e: Exception) {
                println("[iOS OCR] 识别异常: ${e.message}")
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    /**
     * ByteArray 转 NSData
     */
    private fun ByteArray.toNSData(): NSData {
        if (this.isEmpty()) return NSData()
        return this.usePinned { pinned: Pinned<ByteArray> ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }
    
    /**
     * 清理识别结果
     */
    private fun cleanCaptchaText(text: String): String {
        // 移除空格和换行
        val cleaned = text.replace(Regex("[\\s\\n\\r]"), "")
        
        // 保留数字和字母
        val alphanumeric = cleaned.filter { it.isLetterOrDigit() }
        
        // 处理常见的OCR误识别
        val corrected = StringBuilder()
        for (c in alphanumeric) {
            val fixed = when (c) {
                'O', 'o' -> '0'
                'l', 'I', '|' -> '1'
                'Z' -> '2'
                'S', '$' -> '5'
                'B' -> '8'
                'G' -> '6'
                'q' -> '9'
                'D' -> '0'
                else -> c
            }
            corrected.append(fixed)
        }
        
        return corrected.toString().uppercase()
    }
}
