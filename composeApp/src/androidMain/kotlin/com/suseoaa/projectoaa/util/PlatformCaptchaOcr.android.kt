package com.suseoaa.projectoaa.util

/**
 * Android平台的OCR验证码识别实现
 * 使用ML Kit进行文字识别
 */
actual object PlatformCaptchaOcr {
    /**
     * 识别验证码图片中的文字
     */
    actual suspend fun recognize(imageBytes: ByteArray): Result<String> {
        return CaptchaOcrRecognizer.recognizeCaptcha(imageBytes)
    }
}
