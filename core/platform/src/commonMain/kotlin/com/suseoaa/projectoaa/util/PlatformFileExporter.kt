package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 跨平台文件导出器：将导出的二进制文件写入应用缓存/下载目录并唤起系统分享/保存面板。
 */
@Composable
expect fun rememberFileExporter(): (fileName: String, mimeType: String, bytes: ByteArray) -> Result<String>

internal fun sanitizeExportFileName(fileName: String): String {
    val cleaned = fileName.trim()
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .filterNot { it.isISOControl() }
        .replace("..", "")
        .trim()
        .trim('.')
    return cleaned.ifEmpty { "export.xlsx" }
}
