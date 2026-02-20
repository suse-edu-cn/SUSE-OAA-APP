package com.suseoaa.projectoaa

import androidx.compose.ui.window.ComposeUIViewController
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.shared.di.getSharedModules
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(
                getSharedModules() + listOf(
                    platformModule(),
                    appModule
                )
            )
        }
    }
) {
    App()
}
