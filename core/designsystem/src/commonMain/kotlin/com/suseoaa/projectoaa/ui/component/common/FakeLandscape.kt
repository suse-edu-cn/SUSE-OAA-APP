package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout

@Composable
fun FakeLandscape(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val isLandscape = w > h
        if (isLandscape) {
            content()
        } else {
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val childConstraints = constraints.copy(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth
                        )
                        val placeable = measurable.measure(childConstraints)
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            val dx = (constraints.maxWidth - placeable.width) / 2
                            val dy = (constraints.maxHeight - placeable.height) / 2
                            placeable.place(dx, dy)
                        }
                    }
                    .graphicsLayer {
                        rotationZ = 90f
                    }
            ) {
                content()
            }
        }
    }
}
