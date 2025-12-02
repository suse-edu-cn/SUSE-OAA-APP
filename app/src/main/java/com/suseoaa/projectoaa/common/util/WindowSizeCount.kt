package com.suseoaa.projectoaa.common.util

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import javax.inject.Singleton


@Singleton
class WindowSizeCount {
    private var _windowSizeCounts: WindowSizeClass? = null
    val windowsSizeClass: WindowSizeClass
        get() = _windowSizeCounts ?: error("WindowSizeClass未初始化")

    @Composable
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    fun Update(activity: Activity) {
        _windowSizeCounts = calculateWindowSizeClass(activity)
    }
}