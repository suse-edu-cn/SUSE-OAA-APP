package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

@Composable
expect fun LockScreenOrientation(landscape: Boolean)

@Composable
expect fun LockFullscreen(fullscreen: Boolean)
