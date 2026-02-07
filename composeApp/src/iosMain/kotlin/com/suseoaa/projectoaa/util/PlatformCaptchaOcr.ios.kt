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
 * OCR 识别器接口 - 可由 Swift 实现
 */
interface IOSOcrRecognizer {
    fun recognize(imageData: NSData): String?
}

/**
 * 全局 OCR 识别器，可由 Swift 端设置
 */
object IOSOcrRegistry {
    var ocrRecognizer: IOSOcrRecognizer? = null

    fun setRecognizer(recognizer: IOSOcrRecognizer) {
        ocrRecognizer = recognizer
        println("[iOS OCR] 已注册外部 OCR 识别器")
    }
}

/**
 * iOS平台的OCR验证码识别实现
 * 优先使用注册的 Swift OCR（ddddocr），
 * 如果不可用则回退到 Vision Framework
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

                // 优先尝试使用注册的 OCR 识别器（ddddocr）
                val externalOcr = IOSOcrRegistry.ocrRecognizer
                if (externalOcr != null) {
                    val result = externalOcr.recognize(nsData)
                    if (result != null && result.isNotEmpty()) {
                        println("[iOS OCR] ddddocr 识别结果: $result")
                        continuation.resume(Result.success(result))
                        return@suspendCoroutine
                    }
                    println("[iOS OCR] ddddocr 识别失败，回退到 Vision Framework")
                }

                // 回退到 Vision Framework
                val uiImage = UIImage.imageWithData(nsData)
                if (uiImage == null) {
                    continuation.resume(Result.failure(Exception("无法解码图片")))
                    return@suspendCoroutine
                }

                val cgImage = uiImage.CGImage
                if (cgImage == null) {
                    continuation.resume(Result.failure(Exception("无法获取CGImage")))
                    return@suspendCoroutine
                }

                var recognizedText = ""

                val request = VNRecognizeTextRequest { request, error ->
                    if (error != null) {
                        println("[iOS OCR] Vision 识别错误: ${error.localizedDescription}")
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val observations = request?.results as? List<VNRecognizedTextObservation>
                        observations?.forEach { observation ->
                            val candidates = observation.topCandidates(1u)
                            candidates.firstOrNull()?.let { candidate ->
                                @Suppress("UNCHECKED_CAST")
                                val text = (candidate as? Any)?.toString() ?: ""
                                if (text.isNotEmpty() && !text.startsWith("<")) {
                                    recognizedText += text
                                }
                            }
                        }
                    }
                }

                request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
                request.setRecognitionLanguages(listOf("en-US"))
                request.usesLanguageCorrection = false
                request.setMinimumTextHeight(0.0f)

                val handler = VNImageRequestHandler(cgImage, emptyMap<Any?, Any?>())
                val success = handler.performRequests(listOf(request), null)

                if (!success) {
                    continuation.resume(Result.failure(Exception("识别请求执行失败")))
                    return@suspendCoroutine
                }

                val cleanedText = cleanCaptchaText(recognizedText)

                if (cleanedText.isBlank()) {
                    continuation.resume(Result.failure(Exception("未能识别出验证码")))
                } else {
                    println("[iOS OCR] Vision 识别结果: $cleanedText (原始: $recognizedText)")
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
        val cleaned = text.replace(Regex("[\\s\\n\\r]"), "")
        val alphanumeric = cleaned.filter { it.isLetterOrDigit() }

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
