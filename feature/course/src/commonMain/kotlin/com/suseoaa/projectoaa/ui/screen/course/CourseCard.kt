package com.suseoaa.projectoaa.ui.screen.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import com.suseoaa.projectoaa.domain.course.CourseOverlapStatus
import com.suseoaa.projectoaa.presentation.course.ScheduleLayoutItem

// 单张课程卡片，以及把课程折算成网格位置的预处理。

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
internal fun CourseCard(
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
