package com.suseoaa.projectoaa.ui.screen.course

import com.suseoaa.projectoaa.shared.domain.model.course.PracticeCourseEntity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.*

// 课表页的附属控件：缩放弹窗与实训课横幅。

/**
 * 带缩放动画的对话框
 * 从点击位置展开，关闭时缩放回去
 */
@Composable
internal fun ScaleAnimatedDialog(
    onDismissRequest: () -> Unit,
    originBounds: Rect?,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    // 启动时触发动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )

    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dialogAlpha"
    )

    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    // 如果有原点位置，从那个位置作为变换原点
                    if (originBounds != null) {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ==================== 课表布局组件 ====================

/**
 * 整周实践课提示条。
 *
 * 实习这类课程整周进行、没有固定的星期节次，放不进课表格子，
 * 教务系统导出的课表也是把它们单列在表格下方，这里沿用同样的呈现方式。
 * 当前周没有实践课时不占任何高度。
 */
@Composable
internal fun PracticeCourseBanner(
    practiceCourses: List<PracticeCourseEntity>,
    modifier: Modifier = Modifier
) {
    if (practiceCourses.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            practiceCourses.forEach { course ->
                val detail = listOfNotNull(
                    course.teacher.takeIf { it.isNotBlank() },
                    course.weeks.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                Text(
                    text = if (detail.isEmpty()) {
                        "实践课：${course.courseName}"
                    } else {
                        "实践课：${course.courseName}（$detail）"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
