package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.suseoaa.projectoaa.ui.animation.pageShellBounds

/**
 * Generic container for page-level shared transition.
 *
 * Use the same transitionKey on source and destination pages to get
 * consistent enter/exit animation in SharedNavHost.
 *
 * 用的是 [pageShellBounds] 而不是裸的 `sharedBoundsTransition`：共享元素展开动画
 * 在 iOS 上稳定状态的边界会比真实安全区短一截，短的部分（底部 Home Indicator
 * 那条）会露出未着色的内容，看起来像一块黑色。这个容器本来就是给"页面级"整屏
 * 内容用的，直接在这里补上背景色，调用方不用各自处理一遍。
 */
@Composable
fun SharedTransitionPageContainer(
    transitionKey: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pageShellBounds(transitionKey, backgroundColor),
        content = content
    )
}
