package com.suseoaa.projectoaa.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAX_BACKGROUND_IMAGE_BYTES = 240 * 1024

@OptIn(ExperimentalEncodingApi::class)
fun encodeBackgroundImage(imageData: ByteArray): String? {
    if (imageData.isEmpty()) return null
    if (imageData.size > MAX_BACKGROUND_IMAGE_BYTES) return null
    return Base64.Default.encode(imageData)
}

@OptIn(ExperimentalEncodingApi::class)
fun decodeBackgroundImage(encoded: String?): ByteArray? {
    if (encoded.isNullOrBlank()) return null
    return runCatching { Base64.Default.decode(encoded) }.getOrNull()
}
