package com.suseoaa.projectoaa.presentation.course

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.datetime.*
import kotlin.math.roundToInt

/**
 * 课表网格的渲染：周布局、时间轴、背景网格、课程卡片与冲突叠加。
 */

// ==================== 缩放动画对话框 ====================

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

@Composable
internal fun CourseScheduleLayout(
    weekLayoutMap: Map<Int, List<ScheduleLayoutItem>>,
    overlapStatusByWeek: Map<Int, Map<String, CourseOverlapStatus>>,
    overlapFilter: OverlapDisplayFilter,
    onlyShowOverlap: Boolean,
    activeQueryCount: Int,
    accountNameById: Map<String, String>,
    startDate: LocalDate,
    pagerState: PagerState,
    dailySchedule: List<TimeSlotConfig>,
    minWeek: Int = 1,
    bottomPadding: Dp = 0.dp,
    onCourseClick: (List<ScheduleLayoutItem>, Rect?) -> Unit
) {
    val density = LocalDensity.current
    val timeAxisWidth = 40.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // 星期头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.width(timeAxisWidth))
            StaticWeekDayHeader()
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = bottomPadding)  // 应用底部 padding
        ) {
            val totalHeight = maxHeight
            val gridHeight = totalHeight - DateHeaderHeight
            val totalWeight = remember(dailySchedule) {
                dailySchedule.sumOf { it.weight.toDouble() }.toFloat()
            }
            val unitHeightPx = with(density) { gridHeight.toPx() } / totalWeight
            val parentMaxWidth = maxWidth

            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧时间轴
                Surface(
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(DateHeaderHeight))
                        StaticTimeAxis(dailySchedule, unitHeightPx, gridHeight)
                    }
                }

                // 课表网格
                Box(modifier = Modifier.weight(1f)) {
                    // 静态网格背景
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(DateHeaderHeight))
                        StaticGridBackground(dailySchedule, unitHeightPx)
                    }

                    // 周次 Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 2,
                        pageSpacing = 0.dp
                    ) { page ->
                        val weekIndex = page + minWeek
                        val weekStart = remember(startDate, page) {
                            startDate.plus(page * 7, DateTimeUnit.DAY)
                        }
                        val layoutItems = weekLayoutMap[weekIndex] ?: emptyList()


                        DynamicWeekContent(
                            layoutItems = layoutItems,
                            weekStartDate = weekStart,
                            overlapStatusMap = overlapStatusByWeek[weekIndex].orEmpty(),
                            overlapFilter = overlapFilter,
                            onlyShowOverlap = onlyShowOverlap,
                            activeQueryCount = activeQueryCount,
                            accountNameById = accountNameById,
                            unitHeightPx = unitHeightPx,
                            maxWidth = parentMaxWidth - timeAxisWidth,
                            dailySchedule = dailySchedule,
                            onCourseClick = onCourseClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicWeekContent(
    layoutItems: List<ScheduleLayoutItem>,
    weekStartDate: LocalDate,
    overlapStatusMap: Map<String, CourseOverlapStatus>,
    overlapFilter: OverlapDisplayFilter,
    onlyShowOverlap: Boolean,
    activeQueryCount: Int,
    accountNameById: Map<String, String>,
    unitHeightPx: Float,
    maxWidth: Dp,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<ScheduleLayoutItem>, Rect?) -> Unit
) {
    // 按“单日列宽”判断设备可用空间，而不是按平台名称硬编码，适配折叠屏/小窗等场景。
    val isCompactConflictMode = (maxWidth / 7f) < CompactConflictColWidthThreshold

    Column(modifier = Modifier.fillMaxSize()) {
        DynamicDateRow(weekStartDate)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HighlightTodayColumn(weekStartDate, maxWidth)
            ScheduleCourseOverlay(
                items = layoutItems,
                overlapStatusMap = overlapStatusMap,
                overlapFilter = overlapFilter,
                onlyShowOverlap = onlyShowOverlap,
                activeQueryCount = activeQueryCount,
                accountNameById = accountNameById,
                unitHeightPx = unitHeightPx,
                dailySchedule = dailySchedule,
                isCompactConflictMode = isCompactConflictMode,
                onCourseClick = onCourseClick
            )
        }
    }
}

/**
 * 课表课程层：负责把 [ScheduleLayoutItem] 渲染为可点击课程卡片。
 *
 * 渲染策略：
 * - `isCompactConflictMode = true`（手机紧凑模式）
 *   同一冲突组只显示一张主卡片，右上角展示“冲突N”。
 * - `isCompactConflictMode = false`（平板/宽屏）
 *   冲突课程按车道并排显示，避免相互覆盖。
 *
 * 用法示例：
 * `ScheduleCourseOverlay(layoutItems, unitHeightPx, dailySchedule, true, onCourseClick)`
 */
@Composable
private fun ScheduleCourseOverlay(
    items: List<ScheduleLayoutItem>,
    overlapStatusMap: Map<String, CourseOverlapStatus>,
    overlapFilter: OverlapDisplayFilter,
    onlyShowOverlap: Boolean,
    activeQueryCount: Int,
    accountNameById: Map<String, String>,
    unitHeightPx: Float,
    dailySchedule: List<TimeSlotConfig>,
    isCompactConflictMode: Boolean,
    onCourseClick: (List<ScheduleLayoutItem>, Rect?) -> Unit
) {
    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { CardVerticalPadding.toPx() }
    val horizontalPaddingPx = with(density) { CardHorizontalPadding.toPx() }
    val conflictInnerSpacingPx = with(density) { ConflictCardInnerSpacing.toPx() }

    val preparedItems = remember(items, activeQueryCount, accountNameById) {
        buildPreparedCardItems(items, activeQueryCount, accountNameById)
    }
    // 紧凑模式仅展示每组冲突的主卡（laneIndex=0），避免手机端文字被压缩。
    val visiblePreparedItems = remember(preparedItems, isCompactConflictMode) {
        if (!isCompactConflictMode) preparedItems
        else preparedItems.filter { it.laneIndex == 0 }
    }

    val preparedWithOverlapStatus =
        remember(visiblePreparedItems, overlapStatusMap, activeQueryCount) {
            visiblePreparedItems.map { prepared ->
                val status = if (activeQueryCount > 1) {
                    prepared.overlapStatus
                } else {
                    val key = buildScheduleLayoutOverlapKey(prepared.layoutItem)
                    overlapStatusMap[key] ?: CourseOverlapStatus.NO_OVERLAP
                }
                prepared to status
            }
        }

    val filteredPreparedItems =
        remember(preparedWithOverlapStatus, overlapFilter, onlyShowOverlap) {
            preparedWithOverlapStatus.filter { (_, status) ->
                val keepByFilter = status.matchesFilter(overlapFilter)
                val keepBySwitch = !onlyShowOverlap || status != CourseOverlapStatus.NO_OVERLAP
                keepByFilter && keepBySwitch
            }
        }

    Layout(content = {
        filteredPreparedItems.forEach { (prepared, overlapStatus) ->
            val item = prepared.layoutItem
            val conflictData = prepared.conflictGroup

            CourseCard(
                title = prepared.customTitle ?: item.course.course.courseName,
                location = if (prepared.customTitle != null) "" else item.time.location,
                color = prepared.color,
                overlapStatus = overlapStatus,
                isConflict = prepared.conflictGroup.size > 1,
                conflictCount = prepared.conflictGroup.size,
                onClickWithBounds = { bounds -> onCourseClick(conflictData, bounds) },
                modifier = Modifier.layoutId(prepared)
            )
        }
    }) { measurables, constraints ->
        val totalWidthPx = constraints.maxWidth.toFloat()
        val colWidthPx = totalWidthPx / 7f

        val slotYPositions = FloatArray(dailySchedule.size + 1)
        var currentY = 0f
        dailySchedule.forEachIndexed { index, slot ->
            slotYPositions[index] = currentY
            currentY += slot.weight * unitHeightPx
        }
        slotYPositions[dailySchedule.size] = currentY

        val placeables = measurables.map { measurable ->
            val prepared = measurable.layoutId as PreparedCardItem
            val item = prepared.layoutItem
            val yPos = slotYPositions[item.startNodeIndex]
            // 计算实际占用的槽位数量（从startNodeIndex到endNodeIndex的所有槽位）
            val endSlotIndex = (item.endNodeIndex + 1).coerceAtMost(dailySchedule.size)
            val endYPos = slotYPositions[endSlotIndex]

            val availableColWidth = (colWidthPx - horizontalPaddingPx * 2).coerceAtLeast(0f)
            val laneCount = prepared.laneCount.coerceAtLeast(1)
            val laneWidth = if (isCompactConflictMode || laneCount == 1) {
                // 手机端冲突只显示一张主卡片，保持完整宽度
                availableColWidth
            } else {
                (availableColWidth - conflictInnerSpacingPx * (laneCount - 1)).coerceAtLeast(0f) / laneCount
            }
            val laneHeight = (endYPos - yPos - verticalPaddingPx * 2).coerceAtLeast(0f)

            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    width = laneWidth.roundToInt().coerceAtLeast(0),
                    height = laneHeight.roundToInt().coerceAtLeast(0)
                )
            )
            Triple(placeable, prepared, yPos)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, prepared, yPos) ->
                val item = prepared.layoutItem
                val availableColWidth = (colWidthPx - horizontalPaddingPx * 2).coerceAtLeast(0f)
                val laneCount = prepared.laneCount.coerceAtLeast(1)
                val laneWidth = if (isCompactConflictMode || laneCount == 1) {
                    availableColWidth
                } else {
                    (availableColWidth - conflictInnerSpacingPx * (laneCount - 1)).coerceAtLeast(0f) / laneCount
                }
                val laneXOffset =
                    if (isCompactConflictMode) 0f else prepared.laneIndex * (laneWidth + conflictInnerSpacingPx)

                placeable.place(
                    // 水平方向：列起始位置 + 左边距
                    (colWidthPx * item.dayIndex + horizontalPaddingPx + laneXOffset).roundToInt(),
                    // 垂直方向：行起始位置 + 上边距
                    (yPos + verticalPaddingPx).roundToInt()
                )
            }
        }
    }
}

/**
 * 将一周课程转换为可渲染的冲突分组数据。
 *
 * 算法说明：
 * 1. 先按星期分组。
 * 2. 再按时间区间重叠聚类（cluster）。
 * 3. 在每个 cluster 中做车道分配（lane），用于并排显示。
 *
 * 结果由 [ScheduleCourseOverlay] 消费，用于手机/平板两种冲突显示模式。
 */
internal fun buildPreparedCardItems(
    items: List<ScheduleLayoutItem>,
    activeQueryCount: Int = 1,
    accountNameById: Map<String, String> = emptyMap()
): List<PreparedCardItem> {
    val result = mutableListOf<PreparedCardItem>()

    for (day in 0..6) {
        val dayItems = items
            .filter { it.dayIndex == day }
            .sortedWith(compareBy<ScheduleLayoutItem> { it.startNodeIndex }.thenBy { it.endNodeIndex })
        if (dayItems.isEmpty()) continue

        val clusters = mutableListOf<List<ScheduleLayoutItem>>()
        var currentCluster = mutableListOf<ScheduleLayoutItem>()
        var currentClusterMaxEnd = -1

        dayItems.forEach { item ->
            if (currentCluster.isEmpty()) {
                currentCluster.add(item)
                currentClusterMaxEnd = item.endNodeIndex
            } else if (item.startNodeIndex <= currentClusterMaxEnd) {
                currentCluster.add(item)
                currentClusterMaxEnd = maxOf(currentClusterMaxEnd, item.endNodeIndex)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(item)
                currentClusterMaxEnd = item.endNodeIndex
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        clusters.forEach { cluster ->
            if (activeQueryCount > 1) {
                // 共享查询模式：将聚集块分割为原子时间段以精确表示重叠
                val boundaries =
                    cluster.flatMap { listOf(it.startNodeIndex, it.endNodeIndex + 1) }.distinct()
                        .sorted()

                var currentSegmentStart = -1
                var currentSegmentEnd = -1
                var currentSegmentItems = emptyList<ScheduleLayoutItem>()

                fun emitSegment() {
                    if (currentSegmentItems.isEmpty()) return

                    val clusterUniqueAccountsCount =
                        cluster.map { it.course.course.studentId }.distinct().size
                    val uniqueAccountsCount =
                        currentSegmentItems.map { it.course.course.studentId }.distinct().size

                    val status = when {
                        clusterUniqueAccountsCount <= 1 -> CourseOverlapStatus.NO_OVERLAP
                        uniqueAccountsCount >= activeQueryCount -> CourseOverlapStatus.OVERLAP
                        else -> CourseOverlapStatus.PARTIAL_OVERLAP
                    }

                    val accountNames =
                        currentSegmentItems.map { it.course.course.studentId }.distinct()
                            .map { id -> accountNameById[id] ?: id }
                    val accountText = accountNames.joinToString("\n")

                    val statusText = overlapFilterLabel(
                        when (status) {
                            CourseOverlapStatus.NO_OVERLAP -> OverlapDisplayFilter.NO_OVERLAP
                            CourseOverlapStatus.OVERLAP -> OverlapDisplayFilter.OVERLAP
                            CourseOverlapStatus.PARTIAL_OVERLAP -> OverlapDisplayFilter.PARTIAL_OVERLAP
                        }
                    )

                    val representativeItem = currentSegmentItems.minByOrNull { it.startNodeIndex }
                        ?: currentSegmentItems.first()
                    val unifiedItem = representativeItem.copy(
                        startNodeIndex = currentSegmentStart,
                        endNodeIndex = currentSegmentEnd
                    )

                    val baseColor = overlapFilterColor(
                        when (status) {
                            CourseOverlapStatus.NO_OVERLAP -> OverlapDisplayFilter.NO_OVERLAP
                            CourseOverlapStatus.OVERLAP -> OverlapDisplayFilter.OVERLAP
                            CourseOverlapStatus.PARTIAL_OVERLAP -> OverlapDisplayFilter.PARTIAL_OVERLAP
                        }
                    )

                    result.add(
                        PreparedCardItem(
                            layoutItem = unifiedItem,
                            laneIndex = 0,
                            laneCount = 1,
                            conflictGroup = currentSegmentItems,
                            color = baseColor,
                            overlapStatus = status,
                            customTitle = "$statusText\n$accountText"
                        )
                    )
                }

                for (i in 0 until boundaries.size - 1) {
                    val segStart = boundaries[i]
                    val segEnd = boundaries[i + 1] - 1
                    if (segStart > segEnd) continue

                    val segItems =
                        cluster.filter { it.startNodeIndex <= segStart && it.endNodeIndex >= segEnd }
                    if (segItems.isEmpty()) continue

                    // 通过检查是否包含完全相同的课程来匹配项目集
                    val hasSameCourses = currentSegmentItems.size == segItems.size &&
                            currentSegmentItems.map { it.course.course.studentId + it.course.course.courseName }
                                .toSet() ==
                            segItems.map { it.course.course.studentId + it.course.course.courseName }
                                .toSet()

                    if (hasSameCourses && currentSegmentEnd + 1 == segStart) {
                        currentSegmentEnd = segEnd
                    } else {
                        emitSegment()
                        currentSegmentStart = segStart
                        currentSegmentEnd = segEnd
                        currentSegmentItems = segItems
                    }
                }
                emitSegment()
            } else {
                // 正常模式：如果存在物理冲突，将它们放在平行的轨道中
                val laneEnd = mutableListOf<Int>()
                val laneAssignment = mutableMapOf<ScheduleLayoutItem, Int>()

                cluster
                    .sortedWith(compareBy<ScheduleLayoutItem> { it.startNodeIndex }.thenBy { it.endNodeIndex })
                    .forEach { item ->
                        var assignedLane = -1
                        for (lane in laneEnd.indices) {
                            if (item.startNodeIndex > laneEnd[lane]) {
                                assignedLane = lane
                                laneEnd[lane] = item.endNodeIndex
                                break
                            }
                        }
                        if (assignedLane == -1) {
                            laneEnd.add(item.endNodeIndex)
                            assignedLane = laneEnd.lastIndex
                        }
                        laneAssignment[item] = assignedLane
                    }

                val laneCount = laneEnd.size.coerceAtLeast(1)
                cluster.forEach { item ->
                    val courseName = item.course.course.courseName
                    val index = (courseName.hashCode() and Int.MAX_VALUE) % CourseColors.size
                    val baseColor = CourseColors[index]
                    result.add(
                        PreparedCardItem(
                            layoutItem = item,
                            laneIndex = laneAssignment[item] ?: 0,
                            laneCount = laneCount,
                            conflictGroup = cluster,
                            color = if (cluster.size > 1) baseColor.copy(alpha = 0.9f) else baseColor,
                            overlapStatus = CourseOverlapStatus.NO_OVERLAP,
                            customTitle = null
                        )
                    )
                }
            }
        }
    }

    return result
}

@Composable
private fun CourseCard(
    title: String,
    location: String,
    color: Color,
    overlapStatus: CourseOverlapStatus,
    isConflict: Boolean,
    conflictCount: Int,
    onClickWithBounds: (Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInWindow()
            }
            .clickable { onClickWithBounds(cardBounds) }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val cardTextScale = rememberCourseCardTextScale(maxWidth, maxHeight)
            val conflictFontSize = timetableAdaptiveSp(
                baseSp = 8f,
                minSp = 6f,
                compactScale = cardTextScale * 0.9f,
                maxSystemFontScale = 1.05f
            )
            val titleFontSize = timetableAdaptiveSp(
                baseSp = 11f,
                minSp = 7.5f,
                compactScale = cardTextScale
            )
            val titleLineHeight = timetableAdaptiveSp(
                baseSp = 11f,
                minSp = 8f,
                compactScale = cardTextScale * 0.95f
            )
            val locationFontSize = timetableAdaptiveSp(
                baseSp = 9f,
                minSp = 6.5f,
                compactScale = cardTextScale * 0.95f
            )
            val locationLineHeight = timetableAdaptiveSp(
                baseSp = 10f,
                minSp = 7f,
                compactScale = cardTextScale * 0.9f
            )

            if (isConflict) {
                Text(
                    text = "冲突$conflictCount",
                    color = Color.White,
                    fontSize = conflictFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = titleLineHeight,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayLocation = location.removePrefix("L")
                    Text(
                        text = displayLocation,
                        fontSize = locationFontSize,
                        color = Color.White.copy(0.9f),
                        lineHeight = locationLineHeight,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== 静态组件 ====================

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
