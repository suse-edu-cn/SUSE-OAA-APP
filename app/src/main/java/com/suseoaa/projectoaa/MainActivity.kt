package com.suseoaa.projectoaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
// [修复] 引入正确的 ShareViewModel 包名 (navigation.viewmodel)
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
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
                    AppNavigation(
                        windowSizeClass = windowSizeClass.widthSizeClass,
                        viewModel = viewModel<ShareViewModel>()
                    )
                //                旧代码，我怕忘记参数是什么，暂时先保留一下
//                Surface {
//                    // 【步骤3】根据窗口尺寸选择布局
//                    AdaptiveApp(
//                        windowSizeClass = windowSizeClass.widthSizeClass,
//                        viewModel<ShareViewModel>()
//                    )
//                }
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
