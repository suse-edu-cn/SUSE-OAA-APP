package com.suseoaa.projectoaa.common.theme

import android.app.Activity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.common.util.WallpaperManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectOAATheme(
    themeConfig: OaaThemeConfig = ThemeManager.currentTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = themeConfig.colorScheme
    val shapes = themeConfig.shapes
    val view = LocalView.current

    DisposableEffect(view, themeConfig.isDark, colorScheme) {
        val activity = view.context as? Activity
        val componentActivity = view.context as? androidx.activity.ComponentActivity
        val window = activity?.window
        if (window != null) {
            // edge-to-edge（根据需要启用）
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val transparentColor = Color.Transparent.toArgb()
            val backgroundColor = colorScheme.background.toArgb()
            val scrimColor = colorScheme.primary.copy(alpha = 0.2f).toArgb()

            val statusBarStyle = if (themeConfig.isDark) {
                SystemBarStyle.dark(transparentColor)
            } else {
                SystemBarStyle.light(transparentColor, scrimColor)
            }
            val navigationBarStyle = if (themeConfig.isDark) {
                SystemBarStyle.dark(backgroundColor)
            } else {
                SystemBarStyle.light(backgroundColor, scrimColor)
            }

            componentActivity?.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = navigationBarStyle
            )
        }
        onDispose {
            // 可在此恢复原有设置（如果需要）
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            val apiWallpaper by WallpaperManager.currentWallpaper.collectAsState()

            // [Fix] Move file I/O off the main thread using produceState
            val isFileValid by produceState(initialValue = false, key1 = apiWallpaper) {
                if (apiWallpaper?.path != null) {
                    value = withContext(Dispatchers.IO) {
                        val file = File(apiWallpaper!!.path!!)
                        file.exists() && file.length() > 0
                    }
                } else {
                    value = false
                }
            }

            // === 恢复核心逻辑：必须是二次元主题 && 文件有效 才显示 ===
            val showWallpaper = themeConfig.name.contains("二次元") && isFileValid

            if (showWallpaper) {
                // 1. 显示壁纸
                Image(
                    painter = rememberAsyncImagePainter(apiWallpaper),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 2. 叠加半透明蒙层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surface.copy(alpha = 0.9f))
                )
            } else {
                // 3. 非二次元主题（如简约白），或者没图时：显示默认背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background) // 直接使用主题背景色
                )
            }
            content()
        }
    }
}