package com.suseoaa.projectoaa.feature.testScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TestScreen() {
    // 1. 控制详情页是否显示
    var showDetail by remember { mutableStateOf(false) }

    // 2. 记录点击的位置坐标
    var clickCenter by remember { mutableStateOf(Offset.Zero) }

    // 使用 BoxWithConstraints 获取父容器（屏幕）的大小，用于计算比例
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current

        // --- 底层：列表页面 ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize()
        ) {
            items(10) { index ->
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(150.dp)
                        .background(Color.LightGray, RoundedCornerShape(12.dp))
                        // 关键：在这里捕获点击位置
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // 获取相对于整个屏幕的点击位置
                                // 注意：这里需要加上 item 自身的偏移量，或者简单点，
                                // 如果是全屏交互，通常在父级捕获。
                                // 为了演示简单，我们在点击时不仅改变状态，还记录位置。
                            }
                        }
                        // 简单的点击实现，结合 pointerInput 使用时要注意冲突，
                        // 这里我们用一个变通方法：在父容器捕获全局点击更稳妥，
                        // 或者使用 onGloballyPositioned。
                        // 下面这是最简单的“假”实现，为了演示动画原理：
                        .clickable { /* 点击事件交给下面的 Box 统一处理 */ }
                ) {
                    Text("Item $index", modifier = Modifier.align(Alignment.Center))

                    // 透明的覆盖层用于精确捕获点击坐标
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    // 这是一个局部坐标，需要加上当前组件在屏幕的位置
                                    // 为简化代码，本示例演示最通用的"点击屏幕任意位置展开"逻辑
                                    // 在实际列表中，你需要计算 (Item位置 + 触点位置)
                                }
                            }
                    )
                }
            }
        }

        // 为了演示清晰，我们在整个屏幕上覆盖一个点击监听，模拟点击任意 Item
        // 在实际项目中，你会把这个逻辑写在每个 Item 里
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        clickCenter = offset // 记录点击的绝对坐标
                        showDetail = true    // 展开页面
                    }
                }
        )

        // --- 顶层：详情页面 (展开的动画层) ---
        AnimatedVisibility(
            visible = showDetail,
            modifier = Modifier.fillMaxSize(),
            // 3. 定义进入和退出动画
            enter = scaleIn(
                animationSpec = tween(durationMillis = 400),
                initialScale = 0f // 从 0 大小开始
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                animationSpec = tween(durationMillis = 400),
                targetScale = 0f // 缩小回到 0
            ) + fadeOut(tween(200))
        ) {
            // 4. 计算变换原点 (TransformOrigin)
            // 将像素坐标 (px) 转换为 0.0~1.0 的比例
            val originX = with(density) { clickCenter.x / screenWidth.toPx() }
            val originY = with(density) { clickCenter.y / screenHeight.toPx() }

            // 限制范围在 0f 到 1f 之间，防止越界
            val safeOriginX = originX.coerceIn(0f, 1f)
            val safeOriginY = originY.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 【核心代码】：设置缩放的中心点为刚才点击的位置
                        transformOrigin = TransformOrigin(safeOriginX, safeOriginY)
                    }
                    .background(Color(0xFF6200EE))
                    .clickable(
                        // 拦截点击，防止点透到下面
                        interactionSource = null,
                        indication = null
                    ) { /* do nothing */ },
            ) {
                Text(
                    text = "详情页面\n(从点击处展开)",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { showDetail = false }, // 点击关闭，触发 exit 动画
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}