package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.useTabletLayout
import com.suseoaa.projectoaa.ui.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text(text = title, color = titleColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = navigationIconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarContainerColor),
                actions = actions
            )
        }
    ) { paddingValues ->
        AdaptiveLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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

