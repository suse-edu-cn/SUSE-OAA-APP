package com.suseoaa.projectoaa.ui.screen.course

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.suseoaa.projectoaa.presentation.course.CourseViewModel
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

// 课程颜色列表
private val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6)
)

private val DateHeaderHeight = 32.dp

// 时间段类型
enum class SlotType { CLASS, BREAK_LUNCH, BREAK_DINNER }

// 时间段配置
data class TimeSlotConfig(
    val type: SlotType,
    val sectionName: String,
    val startTime: String = "",
    val endTime: String = "",
    val weight: Float
)

// 课程布局项
data class ScheduleLayoutItem(
    val courseName: String,
    val location: String,
    val teacher: String,
    val dayIndex: Int,      // 0-6 周一到周日
    val startNodeIndex: Int, // 起始节索引
    val endNodeIndex: Int,   // 结束节索引
    val weeks: String,
    val period: String
)

// 默认时间表配置
private val defaultDailySchedule = listOf(
    TimeSlotConfig(SlotType.CLASS, "1", "08:00", "08:45", 1f),
    TimeSlotConfig(SlotType.CLASS, "2", "08:55", "09:40", 1f),
    TimeSlotConfig(SlotType.CLASS, "3", "10:00", "10:45", 1f),
    TimeSlotConfig(SlotType.CLASS, "4", "10:55", "11:40", 1f),
    TimeSlotConfig(SlotType.BREAK_LUNCH, "午休", "", "", 0.8f),
    TimeSlotConfig(SlotType.CLASS, "5", "14:00", "14:45", 1f),
    TimeSlotConfig(SlotType.CLASS, "6", "14:55", "15:40", 1f),
    TimeSlotConfig(SlotType.CLASS, "7", "16:00", "16:45", 1f),
    TimeSlotConfig(SlotType.CLASS, "8", "16:55", "17:40", 1f),
    TimeSlotConfig(SlotType.BREAK_DINNER, "晚休", "", "", 0.6f),
    TimeSlotConfig(SlotType.CLASS, "9", "19:00", "19:45", 1f),
    TimeSlotConfig(SlotType.CLASS, "10", "19:55", "20:40", 1f),
    TimeSlotConfig(SlotType.CLASS, "11", "20:50", "21:35", 1f),
    TimeSlotConfig(SlotType.CLASS, "12", "21:45", "22:30", 1f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    viewModel: CourseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 当前选中课程（用于显示详情弹窗）
    var selectedCourseItems by remember { mutableStateOf<List<ScheduleLayoutItem>?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = 90.dp + navBarHeight

    val pagerState = rememberPagerState(
        initialPage = (uiState.currentWeek - 1).coerceAtLeast(0),
        pageCount = { 25 }
    )

    // 监听 Pager 静止状态
    LaunchedEffect(pagerState.settledPage) {
        val newWeek = pagerState.settledPage + 1
        if (uiState.currentWeek != newWeek) {
            viewModel.setCurrentWeek(newWeek)
        }
    }

    // 监听 ViewModel 中的当前周变化
    LaunchedEffect(uiState.currentWeek) {
        val targetPage = uiState.currentWeek - 1
        if (pagerState.currentPage != targetPage && targetPage in 0..24 && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    // 将课程数据转换为布局项
    val weekLayoutMap = remember(uiState.courses) {
        buildWeekLayoutMap(uiState.courses)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 2.dp,
                modifier = Modifier.zIndex(1f)
            ) {
                Column {
                    // 顶部栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp)
                            .padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧标题
                        Column {
                            Text(
                                "课表",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.accountName != null) {
                                Text(
                                    "${uiState.accountName} - ${uiState.className ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // 中间学期选择
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { termDropdownExpanded = true }
                            ) {
                                Text(
                                    uiState.currentTermLabel ?: "当前学期",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = termDropdownExpanded,
                                onDismissRequest = { termDropdownExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                uiState.termOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.updateTermSelection(option.xnm, option.xqm)
                                            termDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 右侧更多菜单
                        Box {
                            Icon(
                                Icons.Default.Add, "更多",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { menuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("刷新当前课表") },
                                    onClick = { menuExpanded = false; viewModel.refreshSchedule() }
                                )
                                DropdownMenuItem(
                                    text = { Text("导入新课表") },
                                    onClick = { menuExpanded = false; showLoginDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("添加自定义课程") },
                                    onClick = { menuExpanded = false; /* TODO */ }
                                )
                                DropdownMenuItem(
                                    text = { Text("查看他人课表") },
                                    onClick = { menuExpanded = false; /* TODO */ }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置开学日期") },
                                    onClick = { menuExpanded = false; /* TODO */ }
                                )
                            }
                        }
                    }

                    // 周次选择器 Tab
                    WeekTabRow(
                        pagerState = pagerState,
                        realCurrentWeek = uiState.realCurrentWeek,
                        onWeekSelected = { week ->
                            scope.launch { pagerState.animateScrollToPage(week - 1) }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = bottomPadding)
                .fillMaxSize()
                .graphicsLayer { clip = true }
        ) {
            if (uiState.courses.isEmpty() && !uiState.isLoading) {
                // 空状态
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "暂无课程数据",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "点击右上角 + 导入课表",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // 课表主体
                CourseScheduleLayout(
                    weekLayoutMap = weekLayoutMap,
                    startDate = uiState.semesterStartDate,
                    pagerState = pagerState,
                    dailySchedule = defaultDailySchedule,
                    onCourseClick = { selectedCourseItems = it }
                )
            }

            // 加载进度条
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        // 课程详情弹窗
        if (selectedCourseItems != null) {
            Dialog(onDismissRequest = { selectedCourseItems = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CourseDetailContent(
                            infoList = selectedCourseItems!!,
                            onClose = { selectedCourseItems = null },
                            modifier = Modifier.wrapContentHeight()
                        )
                    }
                }
            }
        }

        // 登录导入弹窗
        if (showLoginDialog) {
            LoginDialog(
                isLoading = uiState.isLoading,
                onDismiss = { showLoginDialog = false },
                onConfirm = { username, password ->
                    viewModel.fetchAndSaveCourseSchedule(username, password)
                    showLoginDialog = false
                }
            )
        }
    }
}

/**
 * 周次选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekTabRow(
    pagerState: PagerState,
    realCurrentWeek: Int,
    onWeekSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            if (pagerState.currentPage < tabPositions.size) {
                val currentPosition = tabPositions[pagerState.currentPage]
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .width(currentPosition.width)
                        .offset(x = currentPosition.left),
                    height = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        for (w in 1..25) {
            val isSelected = w == (pagerState.currentPage + 1)
            val isRealCurrentWeek = w == realCurrentWeek
            val textColor = when {
                isRealCurrentWeek -> MaterialTheme.colorScheme.tertiary
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            Tab(
                selected = isSelected,
                onClick = { onWeekSelected(w) },
                text = {
                    Text(
                        "${w}周",
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * 课表主布局
 */
@Composable
fun CourseScheduleLayout(
    weekLayoutMap: Map<Int, List<ScheduleLayoutItem>>,
    startDate: LocalDate?,
    pagerState: PagerState,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<ScheduleLayoutItem>) -> Unit
) {
    val density = LocalDensity.current
    val timeAxisWidth = 40.dp
    val effectiveStartDate = startDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

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
        ) {
            val totalHeight = maxHeight
            val gridHeight = totalHeight - DateHeaderHeight
            val totalWeight = remember(dailySchedule) { dailySchedule.sumOf { it.weight.toDouble() }.toFloat() }
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

                // 右侧主体
                Box(modifier = Modifier.weight(1f)) {
                    // 网格背景
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(DateHeaderHeight))
                        StaticGridBackground(dailySchedule, unitHeightPx)
                    }

                    // 周次分页
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 4,
                        pageSpacing = 0.dp
                    ) { page ->
                        val weekIndex = page + 1
                        val weekStart = remember(effectiveStartDate, page) {
                            effectiveStartDate.plus(DatePeriod(days = page * 7))
                        }
                        val layoutItems = weekLayoutMap[weekIndex] ?: emptyList()

                        DynamicWeekContent(
                            layoutItems = layoutItems,
                            weekStartDate = weekStart,
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
fun DynamicWeekContent(
    layoutItems: List<ScheduleLayoutItem>,
    weekStartDate: LocalDate,
    unitHeightPx: Float,
    maxWidth: Dp,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<ScheduleLayoutItem>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DynamicDateRow(weekStartDate)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HighlightTodayColumn(weekStartDate, maxWidth)
            ScheduleCourseOverlay(layoutItems, unitHeightPx, dailySchedule, onCourseClick)
        }
    }
}

/**
 * 课程块覆盖层
 */
@Composable
fun ScheduleCourseOverlay(
    items: List<ScheduleLayoutItem>,
    unitHeightPx: Float,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<ScheduleLayoutItem>) -> Unit
) {
    val preparedGroups = remember(items) {
        items.groupBy { it.dayIndex }.mapValues { (_, dayItems) ->
            dayItems.groupBy { "${it.startNodeIndex}-${it.endNodeIndex}" }
        }
    }

    Layout(content = {
        for (day in 0..6) {
            val dayGroups = preparedGroups[day] ?: emptyMap()
            dayGroups.forEach { (_, groupItems) ->
                val item = groupItems.first()
                val index = (item.courseName.hashCode() and Int.MAX_VALUE) % CourseColors.size
                val baseColor = CourseColors[index]

                CourseCard(
                    title = item.courseName,
                    location = item.location,
                    color = baseColor,
                    onClick = { onCourseClick(groupItems) },
                    modifier = Modifier.layoutId(item)
                )
            }
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
            val item = measurable.layoutId as ScheduleLayoutItem
            val yPos = slotYPositions[item.startNodeIndex]
            val endYPos = slotYPositions[item.endNodeIndex.coerceAtMost(dailySchedule.size)]
            val height = (endYPos - yPos).roundToInt() - 2
            val width = colWidthPx.roundToInt() - 2
            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    width = width.coerceAtLeast(0),
                    height = height.coerceAtLeast(0)
                )
            )
            Triple(placeable, item, yPos)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, item, yPos) ->
                placeable.place(
                    (colWidthPx * item.dayIndex).roundToInt(),
                    yPos.roundToInt()
                )
            }
        }
    }
}

/**
 * 课程卡片
 */
@Composable
fun CourseCard(
    title: String,
    location: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp),
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.85f),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
            if (location.isNotBlank()) {
                Text(
                    text = "@$location",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 星期头部
 */
@Composable
fun StaticWeekDayHeader() {
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        weekDays.forEach { dayName ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 时间轴
 */
@Composable
fun StaticTimeAxis(dailySchedule: List<TimeSlotConfig>, unitHeightPx: Float, height: Dp) {
    val density = LocalDensity.current
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${slot.startTime}\n${slot.endTime}",
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else if (slot.type == SlotType.BREAK_LUNCH || slot.type == SlotType.BREAK_DINNER) {
                        Text(
                            slot.sectionName,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }) { measurables, constraints ->
        val heightPx = with(density) { height.roundToPx() }
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = 100)) }
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

/**
 * 网格背景虚线
 */
@Composable
fun StaticGridBackground(dailySchedule: List<TimeSlotConfig>, unitHeightPx: Float) {
    val dashColor = MaterialTheme.colorScheme.outlineVariant
    val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        var y = 0f
        dailySchedule.forEach { slot ->
            val height = slot.weight * unitHeightPx
            if (slot.type == SlotType.CLASS) {
                drawLine(
                    color = dashColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = pathEffect
                )
            }
            y += height
        }
    }
}

/**
 * 动态日期行
 */
@Composable
fun DynamicDateRow(startDate: LocalDate) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DateHeaderHeight)
            .padding(bottom = 6.dp)
    ) {
        for (i in 0..6) {
            val date = startDate.plus(DatePeriod(days = i))
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
                    fontSize = 11.sp,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 今日高亮列
 */
@Composable
fun HighlightTodayColumn(weekStartDate: LocalDate, maxWidth: Dp) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val daysDiff = today.toEpochDays() - weekStartDate.toEpochDays()
    if (daysDiff in 0..6) {
        val colWidth = maxWidth / 7
        Box(
            modifier = Modifier
                .offset(x = colWidth * daysDiff)
                .width(colWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        )
    }
}

/**
 * 课程详情内容
 */
@Composable
fun CourseDetailContent(
    infoList: List<ScheduleLayoutItem>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (infoList.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { infoList.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.wrapContentHeight()
            ) { page ->
                val item = infoList[page]
                CourseDetailCard(item)
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 课程详情卡片
 */
@Composable
fun CourseDetailCard(item: ScheduleLayoutItem) {
    val details = remember(item) {
        buildList {
            add(DetailInfo(Icons.Default.DateRange, "课程名称", item.courseName))
            if (item.location.isNotBlank()) {
                add(DetailInfo(Icons.Default.Place, "上课地点", item.location))
            }
            if (item.teacher.isNotBlank()) {
                add(DetailInfo(Icons.Default.Person, "教师", item.teacher))
            }
            add(DetailInfo(Icons.Default.DateRange, "时间", item.period))
            add(DetailInfo(Icons.Default.DateRange, "周次", item.weeks))
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
                    rowItems.forEach { infoItem ->
                        Box(modifier = Modifier.weight(1f)) {
                            DetailItem(infoItem.icon, infoItem.label, infoItem.value)
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

data class DetailInfo(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
fun DetailItem(
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

/**
 * 登录导入弹窗
 */
@Composable
fun LoginDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "导入教务系统课表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请输入教务系统账号密码，我们不会存储您的密码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 20) username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Lock else Icons.Default.Lock,
                                contentDescription = null
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(username, password) },
                        enabled = username.isNotBlank() && password.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("导入")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 将课程列表转换为周布局映射
 */
private fun buildWeekLayoutMap(
    courses: List<com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes>
): Map<Int, List<ScheduleLayoutItem>> {
    val result = mutableMapOf<Int, MutableList<ScheduleLayoutItem>>()
    
    courses.forEach { courseWithTimes ->
        courseWithTimes.times.forEach { time ->
            // 解析周次范围
            val weeks = parseWeeks(time.weeks)
            // 解析星期几 (1-7 -> 0-6)
            val dayIndex = (time.weekday.filter { it.isDigit() }.toIntOrNull() ?: 1) - 1
            // 解析节次 (使用 period 字段，格式如 "1-2节")
            val (startNode, endNode) = parseNodeRange(time.period)
            
            weeks.forEach { week ->
                val item = ScheduleLayoutItem(
                    courseName = courseWithTimes.course.courseName,
                    location = time.location,
                    teacher = time.teacher,
                    dayIndex = dayIndex.coerceIn(0, 6),
                    startNodeIndex = startNode,
                    endNodeIndex = endNode,
                    weeks = time.weeks,
                    period = "${time.weekday} ${time.period}"
                )
                result.getOrPut(week) { mutableListOf() }.add(item)
            }
        }
    }
    
    return result
}

/**
 * 解析周次字符串，如 "1-16周" -> [1,2,3,...,16]
 */
private fun parseWeeks(weeksStr: String): List<Int> {
    val result = mutableListOf<Int>()
    val cleaned = weeksStr.replace("周", "").replace("单", "").replace("双", "")
    val isOddOnly = weeksStr.contains("单")
    val isEvenOnly = weeksStr.contains("双")
    
    cleaned.split(",").forEach { part ->
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val (start, end) = trimmed.split("-").map { it.trim().toIntOrNull() ?: 0 }
            for (w in start..end) {
                if (isOddOnly && w % 2 == 0) continue
                if (isEvenOnly && w % 2 == 1) continue
                result.add(w)
            }
        } else {
            trimmed.toIntOrNull()?.let { result.add(it) }
        }
    }
    return result
}

/**
 * 解析节次范围，如 "1-2节" -> (0, 2)
 */
private fun parseNodeRange(nodeRange: String): Pair<Int, Int> {
    val cleaned = nodeRange.replace("节", "").trim()
    return if (cleaned.contains("-")) {
        val parts = cleaned.split("-")
        val start = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val end = parts.getOrNull(1)?.toIntOrNull() ?: start
        // 需要根据实际节次映射到 dailySchedule 索引
        mapNodeToIndex(start) to mapNodeToIndex(end) + 1
    } else {
        val node = cleaned.toIntOrNull() ?: 1
        mapNodeToIndex(node) to mapNodeToIndex(node) + 1
    }
}

/**
 * 将节次映射到 dailySchedule 索引
 * 考虑午休和晚休的插入
 */
private fun mapNodeToIndex(node: Int): Int {
    return when {
        node <= 4 -> node - 1        // 1-4节 -> 索引 0-3
        node <= 8 -> node            // 5-8节 -> 索引 5-8 (跳过午休索引4)
        else -> node + 1             // 9-12节 -> 索引 10-13 (跳过晚休索引9)
    }
}
