package com.suseoaa.projectoaa.ui.component.sukisu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.suseoaa.projectoaa.ui.screen.main.MainTab

import androidx.compose.ui.layout.onSizeChanged

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
    if (isLiquidGlassTabbarEnabled && liquidGlassTabbarStyle == 2) {
        val backdrop = rememberLayerBackdrop()
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(modifier = modifier) {
            content(Modifier.layerBackdrop(backdrop))
            FloatingBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    .onSizeChanged { 
                        // Include the 12dp bottom padding in the reported height
                        val paddingPx = with(density) { 12.dp.roundToPx() }
                        onBottomBarHeightChanged(it.height + paddingPx)
                    },
                selectedIndex = selectedIndex,
                onSelected = onNavigate,
                backdrop = backdrop,
                tabsCount = MainTab.entries.size,
                isBlurEnabled = true
            ) {
                MainTab.entries.forEachIndexed { index, item ->
                    FloatingBottomBarItem(
                        onClick = { onNavigate(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier) {
            content(Modifier)
        }
    }
}
