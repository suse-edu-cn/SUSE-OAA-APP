package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.suseoaa.projectoaa.ui.theme.AppDimensions

@Deprecated(
    message = "Use com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold",
    replaceWith = ReplaceWith("AdaptivePageScaffold", "com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold")
)
@Composable
fun AdaptivePageScaffold(
    title: String,
    onBack: () -> Unit,
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
    com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        compactPadding = compactPadding,
        tabletPadding = tabletPadding,
        containerColor = containerColor,
        topBarContainerColor = topBarContainerColor,
        titleColor = titleColor,
        navigationIconColor = navigationIconColor,
        actions = actions,
        compactContent = compactContent,
        tabletContent = tabletContent
    )
}
