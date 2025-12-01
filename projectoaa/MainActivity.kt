package com.suseoaa.projectoaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // [关键] 引入这个
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suseoaa.projectoaa.common.navigation.AppNavigation
import com.suseoaa.projectoaa.common.theme.ProjectOAATheme
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.common.util.SessionManager
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // [关键修改] 开启 Edge-to-Edge，让应用内容能绘制到状态栏下方
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        SessionManager.fetchToken(this)
        WallpaperManager.initialize(this)

        setContent {
            ProjectOAATheme(themeConfig = ThemeManager.currentTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AppNavigation(windowSizeClass = windowSizeClass.widthSizeClass,
                        viewModel<ShareViewModel>())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ThemeManager.currentTheme.name.contains("二次元")) {
            WallpaperManager.randomizeDisplay(this)
        }
    }
}