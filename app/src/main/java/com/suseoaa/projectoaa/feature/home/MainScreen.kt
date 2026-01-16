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

// 保持定义在顶层，确保 AppNavHost 能引用到它
const val MAIN_SCREEN_ROUTE = "main_screen_route"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass,
    onAcademicEvent: (AcademicPortalEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // 判断设备类型
    val isCompact = windowSizeClass == WindowWidthSizeClass.Compact
    val isTablet = !isCompact

    // 延迟状态读取，防止页面微动时触发全屏重组
    val currentDestinationIndex by remember {
        derivedStateOf { pagerState.currentPage }
    }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2,
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 开启硬件层裁剪，提升滑动帧率
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
                                onNavigateToLogin = onNavigateToLogin
                            )
                        }
                    }
                }

                if (isCompact) {
                    OaaBottomBar(
                        selectedIndex = currentDestinationIndex,
                        onNavigate = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
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