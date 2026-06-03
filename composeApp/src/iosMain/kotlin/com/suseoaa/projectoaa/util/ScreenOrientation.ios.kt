package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

@Composable
actual fun LockScreenOrientation(landscape: Boolean) {
    // 暂不支持 iOS 端代码级别的屏幕强制旋转。
    // 在 iOS 中通常建议由系统和设备物理旋转接管。
    // 若必须在 iOS 强制横屏，需要通过 interop 调用 UIDevice 或直接在视图层用 Modifier.rotate 模拟。
}
