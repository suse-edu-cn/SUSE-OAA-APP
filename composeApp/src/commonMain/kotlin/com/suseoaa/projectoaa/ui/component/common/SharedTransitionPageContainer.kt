package com.suseoaa.projectoaa.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition

/**
 * Generic container for page-level shared transition.
 *
 * Use the same transitionKey on source and destination pages to get
 * consistent enter/exit animation in SharedNavHost.
 */
@Composable
fun SharedTransitionPageContainer(
    transitionKey: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .sharedBoundsTransition(transitionKey),
        content = content
    )
}
