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
    onAcademicEvent: (AcademicPortalEvent) -> Unit
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
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> HomeScreen()
                            1 -> CourseScreen()
                            // 【修复点】这里传入 isTablet 参数
                            2 -> AcademicScreen(
                                isTablet = isTablet,
                                onNavigate = onAcademicEvent
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