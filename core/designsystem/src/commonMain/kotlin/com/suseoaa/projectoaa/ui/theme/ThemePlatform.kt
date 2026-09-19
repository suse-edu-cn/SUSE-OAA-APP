package com.suseoaa.projectoaa.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun platformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    defaultLightScheme: ColorScheme,
    defaultDarkScheme: ColorScheme
): ColorScheme
