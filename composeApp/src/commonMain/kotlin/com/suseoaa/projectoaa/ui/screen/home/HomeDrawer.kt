package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWithDrawer(
    userInfo: PersonData?,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    bottomBarHeight: Dp = 0.dp,
    baseContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var maxPx by remember { mutableFloatStateOf(0f) }
    val peekHeight = 60.dp + bottomBarHeight
    val peekPx = with(density) { peekHeight.toPx() }

    // 使用 Boolean 持久化由于 Animatable 取代带来的纯物理状态
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Animatable：完全不受任何坐标轴范围钳制的物理弹簧机制！
    val offsetYAnim = remember { Animatable(10000f) }
    var isInitialized by remember { mutableStateOf(false) }

    val expandedOffset = maxPx * 0.1f
    val collapsedOffset = maxPx - peekPx
    val dragRange = (collapsedOffset - expandedOffset).coerceAtLeast(1f)

    LaunchedEffect(maxPx, peekPx) {
        if (maxPx > 0f) {
            val target = if (isExpanded) expandedOffset else collapsedOffset
            if (!isInitialized) {
                offsetYAnim.snapTo(target)
                isInitialized = true
            } else if (!offsetYAnim.isRunning) {
                // 当页面从后台切回，测量树重建可能产生极小的临时 maxPx 并在之后恢复
                // 如果此时动画处于静止，我们必须紧紧跟随后续纠正的尺寸，重新正确吸附到预留的 10% 位置上
                offsetYAnim.snapTo(target)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val current = offsetYAnim.value
            val newOffset = current + delta
            // 到达顶层后进一步网上拉提供极佳的摩擦力反馈，而不会像之前那样完全拉不动
            val resistantOffset = if (newOffset < expandedOffset && delta < 0) {
                current + delta * 0.3f
            } else {
                newOffset
            }
            offsetYAnim.snapTo(resistantOffset)
        }
    }

    val settleToTarget = { velocity: Float ->
        coroutineScope.launch {
            val current = offsetYAnim.value
            // 快速划或者慢划的判断逻辑
            val target = if (velocity < -500f) {
                expandedOffset
            } else if (velocity > 500f) {
                collapsedOffset
            } else {
                if (current < collapsedOffset - dragRange * 0.5f) expandedOffset else collapsedOffset
            }

            isExpanded = (target == expandedOffset)

            // 直接将用户挥动的强劲初始动能(velocity)带入动画！没有任何墙限制进度！
            offsetYAnim.animateTo(
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = 0.55f, // 略小阻尼 -> 更活跃的弹跳！绝不受限！
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // 健壮保护尚未获得尺寸闪烁
    val safeOffset = if (!isInitialized || maxPx == 0f) {
        10000f
    } else {
        offsetYAnim.value
    }

    val progress = if (dragRange <= 0f) 0f else {
        (1f - ((safeOffset - expandedOffset) / dragRange)).coerceAtLeast(0f)
    }

    val scaleFactor = 1f - (0.08f * progress)
    val yTranslation = -20f * progress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { size ->
                if (size.height > 0) {
                    maxPx = size.height.toFloat()
                }
            }
    ) {
        // 基底内容视图
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = peekHeight)
                .graphicsLayer {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    translationY = yTranslation
                    shadowElevation = 24.dp.toPx() * progress
                    shape = RoundedCornerShape(36.dp * progress)
                    clip = true
                }
        ) {
            baseContent()
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (0.3f * progress).coerceIn(0f, 1f)))
                )
            }
        }

        // 上滑动抽屉内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, safeOffset.roundToInt()) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> settleToTarget(velocity) }
                )
                .background(
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    "应用功能",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        FeatureCard(
                            name = "招新换届",
                            icon = Icons.Default.GroupAdd,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToRecruitment,
                            sharedBoundKey = "recruitment_feature"
                        )
                    }

                    val invalidRoles = listOf("会员", "普通成员", "")
                    if (userInfo != null && userInfo.role !in invalidRoles) {
                        item {
                            FeatureCard(
                                name = "用户管理",
                                icon = Icons.Default.ManageAccounts,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onColor = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToUserQuery,
                                sharedBoundKey = "user_management_feature"
                            )
                        }
                    }
                }
            }
        }
    }
}
