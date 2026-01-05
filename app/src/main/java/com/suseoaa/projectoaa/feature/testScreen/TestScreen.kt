package com.suseoaa.projectoaa.feature.testScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.*

// 定义两个简单的状态：列表页 OR 详情页
enum class ScreenState { List, Detail }

@Composable
fun TestScreen() {
    var currentScreen by remember { mutableStateOf(ScreenState.List) }

    // 1. 最外层必须包裹 SharedTransitionLayout
    // 这就像是一个舞台，所有的变形魔术都在这里发生
    SharedTransitionLayout {

        // 2. 使用 AnimatedContent 来切换页面
        // 这负责处理页面切换时的淡入淡出逻辑
        AnimatedContent(targetState = currentScreen, label = "screen_transition") { targetState ->

            when (targetState) {
                ScreenState.List -> {
                    // === 列表页 (出发点) ===
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp)
                    ) {
                        // 这里的 Row 就是我们要点击的小卡片
                        Row(
                            modifier = Modifier
                                // 3. 【关键】使用 sharedBounds 标记这个元素
                                // key = "my-card-id" 是身份证，必须和详情页的一样
//                                .sharedBounds(
//                                    sharedContentState = rememberSharedContentState(key = "my-card-id"),
//                                    animatedVisibilityScope = this@AnimatedContent
//                                )
                                .size(100.dp, 60.dp)
                                .background(Color.Blue, RoundedCornerShape(12.dp))
                                .clickable { currentScreen = ScreenState.Detail } // 点击切换状态
                        ) {
                            Text("点击我", color = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }
                }

                ScreenState.Detail -> {
                    // === 详情页 (目的地) ===
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { currentScreen = ScreenState.List } // 点击返回
                    ) {
                        Column(
                            modifier = Modifier
                                // 4. 【关键】给详情页的容器也加上同样的标记
                                // 系统发现两个地方都有 "my-card-id"，就会自动计算变形动画
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "my-card-id"),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .fillMaxSize() // 详情页通常是全屏的
                                .background(Color.Blue) // 保持颜色一致，或者系统会自动渐变颜色
                        ) {
                            Text(
                                "我是详情页的内容",
                                color = Color.White,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}