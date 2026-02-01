package com.suseoaa.projectoaa.util

/**
 * OCR验证码识别器 - 平台通用接口
 */
expect object PlatformCaptchaOcr {
    /**
     * 识别验证码图片中的文字
     * @param imageBytes 验证码图片的字节数组
     * @return 识别出的验证码文字
     */
    suspend fun recognize(imageBytes: ByteArray): Result<String>
}
