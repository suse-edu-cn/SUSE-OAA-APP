package com.suseoaa.projectoaa.common.theme

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.common.util.WallpaperManager
import java.io.File

@Composable
fun ProjectOAATheme(
    themeConfig: OaaThemeConfig = AnimeLightTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = themeConfig.colorScheme
    val shapes = themeConfig.shapes
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !themeConfig.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            val apiWallpaper = WallpaperManager.currentWallpaperUri.value
            val file = if (apiWallpaper?.path != null) File(apiWallpaper.path!!) else null

            // 判断：文件有效
            val isFileValid = apiWallpaper != null && file != null && file.exists() && file.length() > 0

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
                // 3. 非二次元主题，或者没图时：显示默认渐变
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    colorScheme.background
                                )
                            )
                        )
                )
            }

            content()
        }
    }
}