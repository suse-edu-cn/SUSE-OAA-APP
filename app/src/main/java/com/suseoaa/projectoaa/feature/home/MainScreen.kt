package com.suseoaa.projectoaa.feature.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.AcademicScreen
import com.suseoaa.projectoaa.feature.course.CourseScreen
import com.suseoaa.projectoaa.feature.person.PersonScreen
import kotlinx.coroutines.launch

const val MAIN_SCREEN_ROUTE = "main_screen_route"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass,
    onAcademicEvent: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val isCompact = windowSizeClass == WindowWidthSizeClass.Compact
    val isTablet = !isCompact
    val currentDestinationIndex by remember { derivedStateOf { pagerState.currentPage } }

    Row(modifier = modifier.fillMaxSize()) {
        if (!isCompact) {
            OaaNavRail(
                selectedIndex = currentDestinationIndex,
                onNavigate = { index -> scope.launch { pagerState.scrollToPage(index) } }
            )
        }
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2,
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { clip = true }
                    ) {
                        when (page) {
                            0 -> HomeScreen()
                            1 -> CourseScreen()
                            2 -> AcademicScreen(
                                isTablet = isTablet,
                                onNavigate = onAcademicEvent,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )

                            3 -> PersonScreen(
                                onNavigateToLogin = onNavigateToLogin,
                                onNavigateToChangePassword = onNavigateToChangePassword,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }

                if (isCompact) {
                    OaaBottomBar(
                        selectedIndex = currentDestinationIndex,
                        onNavigate = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}