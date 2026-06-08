package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun LiquidGlassBackdropWrapper(
    isLiquidGlassTabbarEnabled: Boolean,
    liquidGlassTabbarStyle: Int,
    selectedIndex: () -> Int,
    onNavigate: (Int) -> Unit,
    onBottomBarHeightChanged: (Int) -> Unit,
    modifier: Modifier,
    content: @Composable (backdropModifier: Modifier) -> Unit
) {
    Box(modifier = modifier) {
        content(Modifier)
    }
}
