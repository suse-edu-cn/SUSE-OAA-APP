package com.suseoaa.projectoaa.feature.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.suseoaa.projectoaa.feature.academicPortal.AcademicPortalEvent
import com.suseoaa.projectoaa.feature.academicPortal.AcademicScreen
import com.suseoaa.projectoaa.feature.course.CourseScreen
import com.suseoaa.projectoaa.feature.person.PersonScreen
import kotlinx.coroutines.launch

// 保持定义在顶层，确保 AppNavHost 能引用到它
const val MAIN_SCREEN_ROUTE = "main_screen_route"

@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass,
    onAcademicEvent: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // 判断设备类型
    val isCompact = windowSizeClass == WindowWidthSizeClass.Compact
    val isTablet = !isCompact // 如果不是紧凑型（手机），就认为是平板/大屏

    val currentDestinationIndex = pagerState.currentPage

    Row(modifier = Modifier.fillMaxSize()) {
        if (!isCompact) {
            OaaNavRail(
                selectedIndex = currentDestinationIndex,
                onNavigate = { index -> scope.launch { pagerState.scrollToPage(index) } }
            )
        }
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .graphicsLayer(clip = false)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(clip = false),
                    beyondViewportPageCount = 2,
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(clip = false)
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

                            3 -> PersonScreen()
                        }
                    }
                }

                if (isCompact) {
                    OaaBottomBar(
                        selectedIndex = currentDestinationIndex,
                        onNavigate = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}