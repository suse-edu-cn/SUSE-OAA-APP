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
import androidx.compose.ui.Modifier
import com.suseoaa.projectoaa.feature.chat.ChatScreen
import com.suseoaa.projectoaa.feature.course.CourseScreen
import com.suseoaa.projectoaa.feature.person.PersonScreen
import kotlinx.coroutines.launch

// 定义主页面的路由常量
const val MAIN_SCREEN_ROUTE = "main_screen_route"

@Composable
fun MainScreen(
    windowSizeClass: WindowWidthSizeClass
) {
    // 1. 定义 Pager 状态，总共 4 页
//    现在改为了3
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // 2. 判断屏幕布局模式（平板 vs 手机）
    val isCompact = windowSizeClass == WindowWidthSizeClass.Compact

    // 3. 当前选中的 Tab 索引（由 Pager 状态决定）
    val currentDestinationIndex = pagerState.currentPage

    Row(modifier = Modifier.fillMaxSize()) {
        // [平板布局] 左侧导航栏
        if (!isCompact) {
            OaaNavRail(
                selectedIndex = currentDestinationIndex,
                onNavigate = { index ->
                    scope.launch { pagerState.scrollToPage(index) }
                }
            )
        }

        Scaffold(
            // [手机布局] 底部导航栏
            bottomBar = {
                if (isCompact) {
                    OaaBottomBar(
                        selectedIndex = currentDestinationIndex,
                        onNavigate = { index ->
                            // 点击底部栏时，Pager 切换页面
                            scope.launch {
                                // 使用 animateScrollToPage 有动画，scrollToPage 是瞬切
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            // 4. 核心容器：HorizontalPager
            // 这里使用了 userScrollEnabled = true 允许手势滑动
            // beyondViewportPageCount = 3 意味着预加载并保持所有 4 个页面的状态！
            // 这彻底解决了你之前提到的“页面销毁导致卡顿”的问题。
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                beyondViewportPageCount = 2
            ) { page ->
                // 根据页码渲染不同的页面
                // 注意：这里我们直接调用 Screen Composable，而不是通过 NavHost
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> HomeScreen()
                        1 -> CourseScreen()
//                        2 -> ChatScreen()
                        2 -> PersonScreen()
                    }
                }
            }
        }
    }
}