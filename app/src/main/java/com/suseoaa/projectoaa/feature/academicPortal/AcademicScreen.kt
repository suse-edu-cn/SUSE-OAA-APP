package com.suseoaa.projectoaa.feature.academicPortal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen
import com.suseoaa.projectoaa.feature.testScreen.ScreenState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class ScreenState { List, Detail }

@Composable
fun AcademicScreen(
    isTablet: Boolean,
    onNavigate: (AcademicPortalEvent) -> Unit
) {
    var currentScreen by remember { mutableStateOf(ScreenState.List) }
    SharedTransitionLayout {
        AnimatedContent(targetState = currentScreen, label = "screen_transition") { targetState ->

            when (targetState) {
                ScreenState.List -> {
                    // === 列表页 (出发点) ===
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // 这里的 Row 就是我们要点击的小卡片
                        Row(
                            modifier = Modifier
                                // 3. 【关键】使用 sharedBounds 标记这个元素
                                // key = "my-card-id" 是身份证，必须和详情页的一样
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "my-card-id"),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .size(100.dp, 60.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(12.dp)
                                )
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
                                .background(color = MaterialTheme.colorScheme.primaryContainer) // 保持颜色一致，或者系统会自动渐变颜色
                        ) {
                            GradesScreen(onBack = { currentScreen = ScreenState.List })
                        }
                    }
                }
            }
        }
    }
}