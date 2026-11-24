package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFileExporter(): (fileName: String, mimeType: String, bytes: ByteArray) -> Result<String> {
    return remember {
        { fileName, _, bytes ->
            runCatching {
                val safeName = sanitizeExportFileName(fileName)
                val targetPath = NSTemporaryDirectory() + safeName
                val nsData = if (bytes.isEmpty()) {
                    NSData()
                } else {
                    bytes.usePinned { pinned ->
                        NSData.create(
                            bytes = pinned.addressOf(0),
                            length = bytes.size.toULong()
                        )
                    }
                }
                check(nsData.writeToFile(targetPath, atomically = true)) {
                    "写入导出文件失败"
                }

                val fileUrl = NSURL.fileURLWithPath(targetPath)
                val topController = findTopViewController()
                    ?: error("无法获取当前界面控制器")

                val activityController = UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null
                )
                activityController.popoverPresentationController?.apply {
                    sourceView = topController.view
                    sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
                }
                topController.presentViewController(
                    activityController,
                    animated = true,
                    completion = null
                )
                "已导出：$safeName"
            }
        }
    }
}

private fun findTopViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val keyWindow = app.keyWindow ?: app.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
    var topController = keyWindow?.rootViewController
    while (topController?.presentedViewController != null) {
        topController = topController.presentedViewController
    }
    return topController
}
