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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.ui.screen.main.MainTab
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * "纯液态玻璃"（liquidGlassTabbarStyle == 2）风格的底部栏容器。
 *
 * 这套效果基于 Miuix（`top.yukonga.miuix.kmp`）的 backdrop 模糊 API 实现，
 * 该库在 Maven Central 上确实发布了 `iosArm64`/`iosSimulatorArm64` 构件，是一个
 * 真正的 KMP 库——此前项目只把它的依赖挂在了 androidMain，[FloatingBottomBar]
 * 及其配套的 `liquid`/`miuix.animation` 辅助文件也整体放在 androidMain 下，
 * 导致这套效果实际上从未在 iOS 上编译过，iOS 端只能用一个空实现兜底，
 * 表现为选中"纯液态玻璃"时 iOS 上整个 Tab 栏消失。
 *
 * 这些文件本身不含任何 Android 专属 API（唯一的例外——高光效果用到的
 * `android.graphics.RuntimeShader`——已经改写成跨平台的 [androidx.compose.ui.graphics.Brush]
 * 径向渐变），所以把依赖和代码一起挪到 commonMain 后，两端共用同一份实现，
 * 不再需要 expect/actual 分别维护。
 */
@Composable
fun LiquidGlassBackdropWrapper(
    isLiquidGlassTabbarEnabled: Boolean,
    liquidGlassTabbarStyle: Int,
    selectedIndex: () -> Int,
    onNavigate: (Int) -> Unit,
    onBottomBarHeightChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (backdropModifier: Modifier) -> Unit
) {
    if (isLiquidGlassTabbarEnabled && liquidGlassTabbarStyle == 2) {
        val backdrop = rememberLayerBackdrop()
        val density = LocalDensity.current
        Box(modifier = modifier) {
            content(Modifier.layerBackdrop(backdrop))
            FloatingBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
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
