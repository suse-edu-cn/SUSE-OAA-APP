package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.ui.animation.pageShellBounds
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.BackButton
import com.suseoaa.projectoaa.ui.component.useTabletLayout
import com.suseoaa.projectoaa.ui.theme.AppDimensions

/**
 * 全站通用的"页面壳子"：顶部安全距离、返回按钮、标题、iOS 底部安全区这些每个二级
 * 页面都要处理一遍的基础样式，统一在这里做一次。
 *
 * ## 为什么会有这个文件
 *
 * 之前全站二级页面分裂成三套并行写法：一部分页面各自手写 `Scaffold + TopAppBar +
 * 返回按钮`；一部分用的是这个文件（当时功能更简单）；还有个别页面完全自己糊一套。
 * 三套写法各自处理"共享元素展开动画" `sharedBoundsTransition` 的方式也不完全一致，
 * 其中一个后果就是：这个动画在 iOS 上稳定状态的边界会短一截，短的部分正好是底部
 * 安全区（Home Indicator 那条），底下没着色的内容透出来就是一块黑色。有个页面
 * （课程信息查询）已经独立发现并手动修过一次，但没有推广到其余页面——这正是三套
 * 写法并存的直接代价：同一个坑要在不同地方各踩一次、各修一次。
 *
 * 现在把这个坑的修复（[pageShellBounds]）直接内置进这个壳子，页面只需要传入
 * `sharedTransitionKey`，不用自己操心 iOS 这块。
 *
 * ## 为什么不用 Material3 的 Scaffold
 *
 * 排查这个壳子迁移的 29 个页面在 iOS 上底部会出现一块遮住内容的圆角色块时，发现
 * [PersonScreen] 早就为同样的症状打过补丁——显式把 `Scaffold` 的
 * `contentWindowInsets` 清零。这说明 Material3 `Scaffold` 在这个 KMP/iOS 目标上
 * 的默认 WindowInsets 处理本身不可靠。与其继续在每个用到 Scaffold 的地方各自打
 * 补丁，这里直接不用 Scaffold，用一个普通的 `Column` + 手写顶栏代替，不做任何隐式
 * 的底部 WindowInsets 预留；哪个页面确实需要避开 iOS Home Indicator，自己按需加
 * `.navigationBarsPadding()`（其余几个页面已经是这么做的）。
 *
 * ## 两种用法
 *
 * - 页面自己不需要按手机/平板分别写不同布局：用 [content] 单内容版本。
 * - 页面在手机和平板上是完全不同的布局结构（比如平板双栏、手机单栏）：用
 *   [compactContent]/[tabletContent] 双内容版本。
 *
 * 返回按钮统一用圆形背景样式（[BackButton]），跟之前部分页面用的纯图标按钮
 * 不再是两种视觉。
 */
@Composable
fun AdaptivePageScaffold(
    title: String,
    onBack: () -> Unit,
    sharedTransitionKey: String,
    modifier: Modifier = Modifier,
    compactPadding: Dp = AppDimensions.screenPaddingCompact,
    tabletPadding: Dp = AppDimensions.screenPaddingMedium,
    containerColor: Color = MaterialTheme.colorScheme.background,
    topBarContainerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIconColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
    compactContent: @Composable (Modifier) -> Unit,
    tabletContent: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .pageShellBounds(sharedTransitionKey, containerColor)
    ) {
        PageShellTopBar(
            title = title,
            onBack = onBack,
            containerColor = topBarContainerColor,
            titleColor = titleColor,
            navigationIconColor = navigationIconColor,
            actions = actions
        )
        AdaptiveLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { config ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(if (config.useTabletLayout()) tabletPadding else compactPadding)
            if (config.useTabletLayout()) {
                tabletContent(contentModifier)
            } else {
                compactContent(contentModifier)
            }
        }
    }
}

/**
 * 单内容版本：页面自身已经用 [AdaptiveLayout] 之类的方式处理手机/平板分支，
 * 不需要壳子再帮它拆一次，只需要外层的顶栏、返回按钮、安全区这些基础样式。
 */
@Composable
fun AdaptivePageScaffold(
    title: String,
    onBack: () -> Unit,
    sharedTransitionKey: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    topBarContainerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIconColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .pageShellBounds(sharedTransitionKey, containerColor)
    ) {
        PageShellTopBar(
            title = title,
            onBack = onBack,
            containerColor = topBarContainerColor,
            titleColor = titleColor,
            navigationIconColor = navigationIconColor,
            actions = actions
        )
        content(Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * 手写顶栏，替代 Material3 `TopAppBar`。顶部安全距离用跟全站其余页面一致的
 * `statusBarsPadding()`，不依赖 Scaffold/TopAppBar 内部那套 WindowInsets 计算。
 */
@Composable
private fun PageShellTopBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color,
    titleColor: Color,
    navigationIconColor: Color,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(
                onClick = onBack,
                tint = navigationIconColor,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = title,
                color = titleColor,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
