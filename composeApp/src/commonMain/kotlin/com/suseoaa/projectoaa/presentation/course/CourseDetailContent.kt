package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import kotlinx.datetime.*

/**
 * 课程详情面板：单节课的详细信息与多课冲突时的对比展示。
 */

// ==================== 课程详情组件 ====================

@Composable
internal fun CourseDetailContent(
    infoList: List<ScheduleLayoutItem>,
    overlapDetailByKey: Map<String, CourseOverlapDetail>,
    activeQueryCount: Int = 1,
    accountNameById: Map<String, String> = emptyMap(),
    onClose: () -> Unit,
    onDelete: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedItemIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = if (infoList.size > 1) "课程详情 (${infoList.size})" else "课程详情"
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                // 删除按钮（仅自定义课程显示）
                if (onDelete != null && infoList.isNotEmpty() && infoList[0].course.course.isCustom) {
                    IconButton(
                        onClick = {
                            val courseEntity = infoList[0].course.course
                            onDelete(courseEntity.courseName, courseEntity.studentId)
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "删除课程",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (infoList.isNotEmpty()) {
            if (activeQueryCount > 1) {
                if (expandedItemIndex != null) {
                    // 显示具体的项目详情
                    Column {
                        Text(
                            text = "返回列表",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { expandedItemIndex = null }
                                .padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val item = infoList[expandedItemIndex!!]
                        val overlapDetail = overlapDetailByKey[buildScheduleLayoutOverlapKey(item)]
                            ?: CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)
                        CourseDetailCard(
                            courseData = item.course,
                            timeData = item.time,
                            overlapDetail = overlapDetail
                        )
                    }
                } else {
                    // 共享查询的列表视图
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(infoList.size) { index ->
                            val item = infoList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedItemIndex = index },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = accountNameById[item.course.course.studentId]
                                                ?: item.course.course.studentId, // Use resolved account name
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.course.course.courseName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.time.location} | ${item.time.classGroup}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "查看详情",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { infoList.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) { page ->
                    val item = infoList[page]
                    val overlapDetail = overlapDetailByKey[buildScheduleLayoutOverlapKey(item)]
                        ?: CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP)
                    CourseDetailCard(
                        courseData = item.course,
                        timeData = item.time,
                        overlapDetail = overlapDetail
                    )
                }
                if (infoList.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        Modifier
                            .wrapContentHeight()
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun CourseDetailCard(
    courseData: CourseWithTimes,
    timeData: ClassTimeEntity,
    overlapDetail: CourseOverlapDetail
) {
    val details = remember(courseData, timeData, overlapDetail) {
        buildList {
            add(DetailInfo(Icons.Default.Star, "课程名称", courseData.course.courseName))
            if (timeData.location.isNotBlank()) {
                add(DetailInfo(Icons.Default.Place, "上课地点", timeData.location))
            }
            if (timeData.teacher.isNotBlank()) {
                add(DetailInfo(Icons.Default.Person, "教师", timeData.teacher))
            }

            // 人性化显示时间
            val weekdayText = formatWeekday(timeData.weekday)
            val periodText = formatPeriod(timeData.period)
            add(DetailInfo(Icons.Default.Refresh, "时间", "$weekdayText $periodText"))

            // 周次
            add(DetailInfo(Icons.Default.DateRange, "周次", timeData.weeks))

            if (!courseData.course.isCustom) {
                // 课程性质
                if (courseData.course.nature.isNotBlank()) {
                    add(DetailInfo(Icons.Default.Info, "课程性质", courseData.course.nature))
                }
                // 课程类型/类别
                if (courseData.course.category.isNotBlank()) {
                    add(DetailInfo(Icons.Default.Menu, "课程类型", courseData.course.category))
                }
                // 考核方式
                if (courseData.course.assessment.isNotBlank()) {
                    add(DetailInfo(Icons.Default.Edit, "考核方式", courseData.course.assessment))
                }
                // 上课班级
                if (timeData.classGroup.isNotBlank()) {
                    add(
                        DetailInfo(
                            Icons.Default.Person,
                            "上课班级",
                            timeData.classGroup.replace(";", "\n")
                        )
                    )
                }
            }

            add(
                DetailInfo(
                    Icons.Default.Check,
                    "重合状态",
                    when (overlapDetail.status) {
                        CourseOverlapStatus.NO_OVERLAP -> "未重合"
                        CourseOverlapStatus.OVERLAP -> "重合"
                        CourseOverlapStatus.PARTIAL_OVERLAP -> "不完全重合"
                    }
                )
            )

            if (overlapDetail.overlappedAccounts.isNotEmpty()) {
                add(
                    DetailInfo(
                        Icons.Default.Person,
                        "重合账号",
                        overlapDetail.overlappedAccounts.joinToString("\n")
                    )
                )
            }

            if (overlapDetail.overlappedCourses.isNotEmpty()) {
                add(
                    DetailInfo(
                        Icons.Default.Menu,
                        "重合课程",
                        overlapDetail.overlappedCourses.joinToString("\n")
                    )
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            val rows = details.chunked(2)
            rows.forEachIndexed { index, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            DetailItem(item.icon, item.label, item.value)
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (index < rows.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 将星期数字转换为中文表示
 */
private fun formatWeekday(weekday: String): String {
    return when (weekday.trim()) {
        "1", "星期一", "周一" -> "星期一"
        "2", "星期二", "周二" -> "星期二"
        "3", "星期三", "周三" -> "星期三"
        "4", "星期四", "周四" -> "星期四"
        "5", "星期五", "周五" -> "星期五"
        "6", "星期六", "周六" -> "星期六"
        "7", "星期日", "星期天", "周日" -> "星期日"
        else -> "星期$weekday"
    }
}

/**
 * 将节次格式化为更友好的显示
 * 例如：1-2 -> 第1-2节，3-4 -> 第3-4节
 */
private fun formatPeriod(period: String): String {
    val cleanPeriod = period.replace("节", "").trim()
    return if (cleanPeriod.isNotBlank()) {
        "第${cleanPeriod}节"
    } else {
        period
    }
}

internal data class DetailInfo(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
internal fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value.ifBlank { "无" },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}
