package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun LiquidGlassBackdropWrapper(
    isLiquidGlassTabbarEnabled: Boolean,
    liquidGlassTabbarStyle: Int,
    selectedIndex: () -> Int,
    onNavigate: (Int) -> Unit,
    onBottomBarHeightChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (backdropModifier: Modifier) -> Unit
)
