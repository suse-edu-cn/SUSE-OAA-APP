package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.shared.di.getSharedModules
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initializeKoinIfNeeded() {
    try {
        KoinPlatform.getKoin()
    } catch (_: Throwable) {
        try {
            startKoin {
                modules(
                    getSharedModules() + listOf(platformModule(), appModule)
                )
            }
        } catch (_: Throwable) {
            // Already started concurrently
        }
    }
}

