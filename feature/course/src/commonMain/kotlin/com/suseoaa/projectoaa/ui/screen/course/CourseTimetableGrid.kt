package com.suseoaa.projectoaa.ui.screen.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.datetime.*
import com.suseoaa.projectoaa.domain.course.SlotType
import com.suseoaa.projectoaa.domain.course.TimeSlotConfig

// 课表的静态骨架：星期表头、时间轴、网格线与今日高亮。

@Composable
internal fun StaticWeekDayHeader() {
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val weekDayFontSize = timetableAdaptiveSp(
        baseSp = 12f,
        minSp = 10f,
        maxSystemFontScale = 1.1f
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        weekDays.forEach { dayName ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = dayName,
                    fontSize = weekDayFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
internal fun StaticTimeAxis(dailySchedule: List<TimeSlotConfig>, unitHeightPx: Float, height: Dp) {
    val sectionFontSize = timetableAdaptiveSp(
        baseSp = 14f,
        minSp = 10f,
        maxSystemFontScale = 1.1f
    )
    val timeFontSize = timetableAdaptiveSp(
        baseSp = 9f,
        minSp = 7f,
        maxSystemFontScale = 1.05f
    )
    val breakFontSize = timetableAdaptiveSp(
        baseSp = 10f,
        minSp = 8f,
        maxSystemFontScale = 1.05f
    )

    Layout(content = {
        dailySchedule.forEach { slot ->
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (slot.type == SlotType.CLASS) {
                        Text(
                            text = slot.sectionName,
                            fontSize = sectionFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${slot.startTime}\n${slot.endTime}",
                            fontSize = timeFontSize,
                            lineHeight = timeFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else if (slot.type == SlotType.BREAK_LUNCH) {
                        Text(
                            slot.sectionName,
                            fontSize = breakFontSize,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }) { measurables, constraints ->
        val heightPx = height.roundToPx()
        val placeables =
            measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = 100)) }
        layout(constraints.maxWidth, heightPx) {
            var y = 0f
            placeables.forEachIndexed { index, placeable ->
                val slot = dailySchedule[index]
                val slotHeight = slot.weight * unitHeightPx
                val x = (constraints.maxWidth - placeable.width) / 2
                val yPos = y + (slotHeight - placeable.height) / 2
                placeable.place(x, yPos.toInt())
                y += slotHeight
            }
        }
    }
}

@Composable
internal fun StaticGridBackground(dailySchedule: List<TimeSlotConfig>, unitHeightPx: Float) {
    val dashColor = MaterialTheme.colorScheme.outlineVariant
    val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        var y = 0f
        var prevWasClass = false
        dailySchedule.forEach { slot ->
            val height = slot.weight * unitHeightPx
            if (slot.type == SlotType.CLASS) {
                // 在每个课程槽位的顶部绘制虚线
                drawLine(
                    color = dashColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = pathEffect
                )
                prevWasClass = true
            } else {
                // 如果上一个是 CLASS，则在当前非 CLASS 槽位的顶部绘制虚线（上一节课的底部）
                if (prevWasClass) {
                    drawLine(
                        color = dashColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }
                prevWasClass = false
            }
            y += height
        }
        // 最后一行底部绘制虚线
        val lastSlot = dailySchedule.lastOrNull()
        if (lastSlot?.type == SlotType.CLASS) {
            drawLine(
                color = dashColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
        }
    }
}

@Composable
internal fun rememberCurrentDate(): LocalDate {
    var today by remember {
        mutableStateOf(
            com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                today = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(60_000L)
            today = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        }
    }
    return today
}

@Composable
internal fun DynamicDateRow(startDate: LocalDate) {
    val today = rememberCurrentDate()
    val dateFontSize = timetableAdaptiveSp(
        baseSp = 11f,
        minSp = 9f,
        maxSystemFontScale = 1.1f
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DateHeaderHeight)
            .padding(bottom = 6.dp)
    ) {
        for (i in 0..6) {
            val date = startDate.plus(i, DateTimeUnit.DAY)
            val isToday = date == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${date.monthNumber}/${date.dayOfMonth}",
                    fontSize = dateFontSize,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
internal fun HighlightTodayColumn(weekStartDate: LocalDate, maxWidth: Dp) {
    val today = rememberCurrentDate()
    val daysBetween = weekStartDate.daysUntil(today)
    if (daysBetween in 0..6) {
        val density = LocalDensity.current
        val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val colWidthPx = totalWidthPx / 7
            val xPx = colWidthPx * daysBetween
            drawRect(
                color = highlightColor,
                topLeft = Offset(xPx, 0f),
                size = Size(colWidthPx, size.height)
            )
        }
    }
}
