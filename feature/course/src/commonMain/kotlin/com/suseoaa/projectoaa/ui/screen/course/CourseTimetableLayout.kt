package com.suseoaa.projectoaa.ui.screen.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlin.math.roundToInt
import com.suseoaa.projectoaa.domain.course.CourseOverlapStatus
import com.suseoaa.projectoaa.presentation.course.ScheduleLayoutItem
import com.suseoaa.projectoaa.presentation.course.buildScheduleLayoutOverlapKey
import com.suseoaa.projectoaa.domain.course.TimeSlotConfig

// 课表主体：周内容与课程覆盖层的排布。

/**
 * 课表网格的渲染：周布局、时间轴、背景网格、课程卡片与冲突叠加。
 */

// ==================== 缩放动画对话框 ====================

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
