package com.suseoaa.projectoaa.util

import android.content.ClipData
import android.content.Intent
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberFileExporter(): (fileName: String, mimeType: String, bytes: ByteArray) -> Result<String> {
    val context = LocalContext.current
    return remember(context) {
        { fileName, mimeType, bytes ->
            runCatching {
                val safeName = sanitizeExportFileName(fileName)
                val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                if (!exportDir.exists()) exportDir.mkdirs()
                val file = File(exportDir, safeName)
                file.writeBytes(bytes)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val resolvedMime = mimeType.ifBlank { "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = resolvedMime
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, safeName)
                    clipData = ClipData.newRawUri(safeName, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "导出文件").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                "已导出：$safeName"
            }
        }
    }
}
