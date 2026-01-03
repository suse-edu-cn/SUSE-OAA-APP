package com.suseoaa.projectoaa.feature.home

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
import com.suseoaa.projectoaa.feature.academicPortal.AcademicScreen
import com.suseoaa.projectoaa.feature.course.CourseScreen
import com.suseoaa.projectoaa.feature.person.PersonScreen
import com.suseoaa.projectoaa.feature.home.HomeScreen
import kotlinx.coroutines.launch

const val MAIN_SCREEN_ROUTE = "main_screen_route"

@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val isCompact = windowSizeClass == WindowWidthSizeClass.Compact
    val currentDestinationIndex = pagerState.currentPage

    Row(modifier = Modifier.fillMaxSize()) {
        if (!isCompact) {
            OaaNavRail(
                selectedIndex = currentDestinationIndex,
                onNavigate = { index -> scope.launch { pagerState.scrollToPage(index) } }
            )
        }
        Scaffold{ padding ->
            // 使用 Box 来实现层叠布局 (Overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding) // 这里只处理系统状态栏等 padding，不再包含 bottomBar 高度
            ) {
                // 1. 底层：页面内容
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> HomeScreen()
                            1 -> CourseScreen()
                            2 -> AcademicScreen()
                            3 -> PersonScreen()
                        }
                    }
                }

                // 2. 顶层：悬浮的底部导航栏
                if (isCompact) {
                    OaaBottomBar(
                        selectedIndex = currentDestinationIndex,
                        onNavigate = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        // 将它对齐到底部
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}