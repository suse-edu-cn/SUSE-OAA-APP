package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

/**
 * 课表界面内部共享的常量、样式尺寸与轻量模型。
 *
 * 这些声明被课表主界面、课表网格与冲突详情等多个文件共用，
 * 因此统一放在本文件并以 internal 可见性暴露给模块内部。
 */
internal val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6)
)

internal val DateHeaderHeight = 32.dp

// 课程卡片间距配置
internal val CardVerticalPadding = 2.dp  // 上下各留的间距
internal val CardHorizontalPadding = 1.dp  // 左右各留的间距
internal val ConflictCardInnerSpacing = 2.dp

// 当单列宽度低于此阈值时，启用手机端冲突策略：仅显示一张主卡片 + 冲突角标。
internal val CompactConflictColWidthThreshold = 62.dp

@Composable
internal fun timetableAdaptiveSp(
    baseSp: Float,
    minSp: Float,
    compactScale: Float = 1f,
    maxSystemFontScale: Float = 1.15f
): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    val systemScaleCompensation =
        if (fontScale > maxSystemFontScale) maxSystemFontScale / fontScale else 1f
    val scaledSp = (baseSp * compactScale * systemScaleCompensation).coerceIn(minSp, baseSp)
    return scaledSp.sp
}

@Composable
internal fun rememberCourseCardTextScale(maxWidth: Dp, maxHeight: Dp): Float =
    remember(maxWidth, maxHeight) {
        when {
            maxWidth < 34.dp || maxHeight < 44.dp -> 0.72f
            maxWidth < 40.dp || maxHeight < 52.dp -> 0.8f
            maxWidth < 48.dp || maxHeight < 64.dp -> 0.9f
            else -> 1f
        }
    }

/**
 * 课表卡片的预处理布局数据。
 *
 * 用法：
 * 1. 由 [buildPreparedCardItems] 生成。
 * 2. 在 [ScheduleCourseOverlay] 中通过 laneIndex/laneCount 决定平板并排布局。
 * 3. 在手机端紧凑模式下，仅保留 laneIndex=0 的主卡片渲染。
 */
@Immutable
internal data class PreparedCardItem(
    val layoutItem: ScheduleLayoutItem,
    val laneIndex: Int,
    val laneCount: Int,
    val conflictGroup: List<ScheduleLayoutItem>,
    val color: Color,
    val overlapStatus: CourseOverlapStatus = CourseOverlapStatus.NO_OVERLAP,
    val customTitle: String? = null
)

internal enum class OverlapDisplayFilter {
    ALL,
    NO_OVERLAP,
    OVERLAP,
    PARTIAL_OVERLAP
}

internal data class OverlapLegendCount(
    val total: Int,
    val noOverlap: Int,
    val overlap: Int,
    val partialOverlap: Int
)

internal data class CourseDetailSelection(
    val items: List<ScheduleLayoutItem>,
    val overlapDetailByKey: Map<String, CourseOverlapDetail>
)

internal fun CourseOverlapStatus.matchesFilter(filter: OverlapDisplayFilter): Boolean {
    return when (filter) {
        OverlapDisplayFilter.ALL -> true
        OverlapDisplayFilter.NO_OVERLAP -> this == CourseOverlapStatus.NO_OVERLAP
        OverlapDisplayFilter.OVERLAP -> this == CourseOverlapStatus.OVERLAP
        OverlapDisplayFilter.PARTIAL_OVERLAP -> this == CourseOverlapStatus.PARTIAL_OVERLAP
    }
}

internal fun overlapFilterColor(filter: OverlapDisplayFilter): Color {
    return when (filter) {
        OverlapDisplayFilter.ALL -> Color(0xFF546E7A)
        OverlapDisplayFilter.NO_OVERLAP -> Color(0xFF26A69A)
        OverlapDisplayFilter.OVERLAP -> Color(0xFFE53935)
        OverlapDisplayFilter.PARTIAL_OVERLAP -> Color(0xFFF9A825)
    }
}

internal fun overlapFilterLabel(filter: OverlapDisplayFilter): String {
    return when (filter) {
        OverlapDisplayFilter.ALL -> "全部"
        OverlapDisplayFilter.NO_OVERLAP -> "未重合"
        OverlapDisplayFilter.OVERLAP -> "重合"
        OverlapDisplayFilter.PARTIAL_OVERLAP -> "不完全重合"
    }
}
