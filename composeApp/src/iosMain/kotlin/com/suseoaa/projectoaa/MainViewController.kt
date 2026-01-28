package com.suseoaa.projectoaa

import androidx.compose.ui.window.ComposeUIViewController
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        // 初始化 Koin
        startKoin {
            modules(
                platformModule(),
                appModule
            )
        }
    }
) {
    App()
}
