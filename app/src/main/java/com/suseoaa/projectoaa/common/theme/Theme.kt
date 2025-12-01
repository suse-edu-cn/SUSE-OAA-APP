package com.suseoaa.projectoaa.common.theme

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.common.util.WallpaperManager
import java.io.File

@Composable
fun ProjectOAATheme(
    themeConfig: OaaThemeConfig = ThemeManager.currentTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = themeConfig.colorScheme
    val shapes = themeConfig.shapes
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // [修改] 沉浸式核心：状态栏背景设为透明
            window.statusBarColor = Color.Transparent.toArgb()

            // [修改] 导航栏(底部)背景设为主题背景色
            window.navigationBarColor = colorScheme.background.toArgb()

            // [修改] 状态栏图标颜色控制：
            // 如果不是暗黑主题(isDark=false)，则 isAppearanceLightStatusBars=true (意味着背景是亮的，系统会把图标显示为黑色)
            // 这样在“简约白”主题下，状态栏图标就是黑色的。
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !themeConfig.isDark
            insetsController.isAppearanceLightNavigationBars = !themeConfig.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            val apiWallpaper by WallpaperManager.currentWallpaper.collectAsState()
            val file = apiWallpaper?.path?.let { File(it) }

            // 判断：文件有效
            val isFileValid = file?.exists() == true && file.length() > 0

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