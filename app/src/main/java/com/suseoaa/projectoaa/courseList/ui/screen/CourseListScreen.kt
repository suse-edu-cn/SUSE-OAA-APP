package com.suseoaa.projectoaa.courseList.ui.screen

import android.app.DatePickerDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.viewmodel.CourseListViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// 预定义的课程颜色
private val CourseColors = listOf(
    Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF64B5F6), Color(0xFF4DB6AC),
    Color(0xFFFFB74D), Color(0xFFAED581), Color(0xFF9575CD), Color(0xFF4DD0E1)
)

data class TimeSlotConfig(
    val sectionName: String, val startTime: String, val type: SlotType, val weight: Float
)

enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

private val DailySchedule = listOf(
    TimeSlotConfig("1", "08:30", SlotType.CLASS, 1.2f),
    TimeSlotConfig("2", "09:20", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("3", "10:25", SlotType.CLASS, 1.2f),
    TimeSlotConfig("4", "11:15", SlotType.CLASS, 1.2f),
    TimeSlotConfig("午餐", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("6", "14:50", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("7", "15:55", SlotType.CLASS, 1.2f),
    TimeSlotConfig("8", "16:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", SlotType.BREAK_DINNER, 0.4f),
    TimeSlotConfig("9", "19:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("10", "19:50", SlotType.CLASS, 1.2f),
    TimeSlotConfig("11", "20:40", SlotType.CLASS, 1.2f)
)

private val SectionIndexMap = DailySchedule.mapIndexedNotNull { index, slot ->
    if (slot.sectionName.isNotEmpty()) slot.sectionName to index else null
}.toMap()

// === 关键修复：定义日期行固定高度 ===
private val DateHeaderHeight = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: CourseListViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val startDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val currentStudentId by viewModel.currentStudentId.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState

    val realCurrentWeek = viewModel.realCurrentWeek
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    var selectedCourses by remember {
        mutableStateOf<List<Pair<CourseWithTimes, ClassTimeEntity>>?>(
            null
        )
    }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = (viewModel.currentDisplayWeek - 1).coerceAtLeast(0),
        pageCount = { 25 }
    )

    // 监听 Pager 滑动 settledPage，避免动画冲突
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val newWeek = page + 1
            if (viewModel.currentDisplayWeek != newWeek) {
                viewModel.currentDisplayWeek = newWeek
            }
        }
    }

    LaunchedEffect(viewModel.currentDisplayWeek) {
        val targetPage = viewModel.currentDisplayWeek - 1
        if (pagerState.currentPage != targetPage && targetPage in 0..24) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    val datePickerDialog = remember {
        val today = LocalDate.now()
        DatePickerDialog(context, { _, y, m, d ->
            viewModel.setSemesterStartDate(LocalDate.of(y, m + 1, d))
        }, today.year, today.monthValue - 1, today.dayOfMonth)
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("课表", fontSize = 16.sp, color = Color.Gray)
                        if (currentStudentId.isNotEmpty()) {
                            Text(currentStudentId, fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                    val currentYear = LocalDate.now().year
                    Text(
                        "$currentYear-${currentYear + 1} 上学期",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Box {
                        Icon(
                            Icons.Default.Add,
                            "更多",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { menuExpanded = true })
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入/更新课表") },
                                onClick = { menuExpanded = false; showLoginDialog = true })
                            DropdownMenuItem(
                                text = { Text("设置起始周") },
                                onClick = { menuExpanded = false; datePickerDialog.show() })
                            DropdownMenuItem(
                                text = { Text("查看他人课表") },
                                onClick = { menuExpanded = false; showAccountDialog = true })
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    divider = {},
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                height = 3.dp,
                                color = Color(0xFFE57373)
                            )
                        }
                    }
                ) {
                    for (w in 1..25) {
                        val isSelected = w == (pagerState.currentPage + 1)
                        val isRealCurrentWeek = w == realCurrentWeek
                        val textColor = when {
                            isRealCurrentWeek -> Color.Red
                            isSelected -> Color(0xFFE57373)
                            else -> Color.Gray
                        }
                        val fontWeight =
                            if (isSelected || isRealCurrentWeek) FontWeight.Bold else FontWeight.Normal

                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.currentDisplayWeek = w },
                            text = {
                                Text(
                                    "${w}周",
                                    fontSize = if (isSelected) 18.sp else 14.sp,
                                    fontWeight = fontWeight,
                                    color = textColor
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            if (allCourses.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无课程数据", color = Color.Gray)
                        Text("请点击右上角 + 号导入", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(if (isTablet && selectedCourses != null) 0.65f else 1f)
                            .fillMaxHeight()
                    ) {
                        CourseScheduleLayout(
                            allCourses = allCourses,
                            startDate = startDate,
                            pagerState = pagerState,
                            getCoursesForWeek = viewModel::getCoursesForWeek,
                            onCourseClick = { selectedCourses = it }
                        )
                    }

                    if (isTablet && selectedCourses != null) {
                        VerticalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            CourseDetailContent(selectedCourses!!) { selectedCourses = null }
                        }
                    }
                }
            }

            if (!isTablet && selectedCourses != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedCourses = null },
                    containerColor = Color.White
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CourseDetailContent(selectedCourses!!) { selectedCourses = null }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        if (showLoginDialog) LoginDialog({
            showLoginDialog = false
        }) { u, p -> viewModel.fetchAndSaveCourseSchedule(u, p); showLoginDialog = false }
        if (showAccountDialog) AccountSelectionDialog(
            savedAccounts,
            currentStudentId,
            { viewModel.switchUser(it); showAccountDialog = false },
            { showAccountDialog = false; showLoginDialog = true }) { showAccountDialog = false }
    }
}

// === 布局容器 ===
@Composable
fun CourseScheduleLayout(
    allCourses: List<CourseWithTimes>,
    startDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    getCoursesForWeek: (Int, List<CourseWithTimes>) -> List<CourseWithTimes>,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
) {
    val density = LocalDensity.current
    val timeAxisWidth = 32.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 顶部：固定的星期标头
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(timeAxisWidth))
            StaticWeekDayHeader()
        }

        // 2. 主体
        BoxWithConstraints(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            // 关键修复：计算实际可用的网格高度
            // 总高度减去顶部的日期行高度，剩余的才是给格子和时间轴的
            val totalHeight = maxHeight
            val gridHeight = totalHeight - DateHeaderHeight
            val totalWeight = remember { DailySchedule.sumOf { it.weight.toDouble() }.toFloat() }

            // 单元格高度基于 gridHeight 计算，而不是 totalHeight
            val unitHeightPx = with(density) { gridHeight.toPx() } / totalWeight

            val parentMaxWidth = maxWidth

            Row(modifier = Modifier.fillMaxSize()) {
                // 3. 左侧：固定的时间轴
                // 使用 Column + Spacer 将其下移，对齐到日期行下方
                Column(modifier = Modifier.width(timeAxisWidth)) {
                    Spacer(modifier = Modifier.height(DateHeaderHeight))
                    StaticTimeAxis(unitHeightPx, gridHeight) // 传入 gridHeight
                }

                // 4. 右侧：可滑动区域
                Box(modifier = Modifier.weight(1f)) {
                    // A. 底层网格 (静态)
                    // 同样下移，对齐
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(DateHeaderHeight))
                        StaticGridBackground(unitHeightPx)
                    }

                    // B. 顶层 Pager (动态)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        pageSpacing = 0.dp
                    ) { page ->
                        val weekIndex = page + 1
                        val weekStart =
                            remember(startDate, page) { startDate.plusWeeks(page.toLong()) }
                        val weekCourses = remember(weekIndex, allCourses) {
                            getCoursesForWeek(
                                weekIndex,
                                allCourses
                            )
                        }

                        DynamicWeekContent(
                            courses = weekCourses,
                            weekStartDate = weekStart,
                            unitHeightPx = unitHeightPx,
                            maxWidth = parentMaxWidth - timeAxisWidth, // 准确的宽度
                            onCourseClick = onCourseClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaticWeekDayHeader() {
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        weekDays.forEach { dayName ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StaticTimeAxis(unitHeightPx: Float, height: Dp) {
    Layout(content = {
        DailySchedule.forEach { slot ->
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (slot.type == SlotType.CLASS) {
                        Text(
                            slot.sectionName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(slot.startTime, fontSize = 9.sp, color = Color.Gray, lineHeight = 9.sp)
                    } else if (slot.type == SlotType.BREAK_LUNCH) {
                        Text(
                            slot.sectionName,
                            fontSize = 10.sp,
                            color = Color(0xFF909090),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }) { measurables, constraints ->
        // 使用传入的 height 约束布局高度
        val heightPx = height.roundToPx()
        val placeables =
            measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = 100)) }

        layout(constraints.maxWidth, heightPx) {
            var y = 0f
            placeables.forEachIndexed { index, placeable ->
                val slot = DailySchedule[index]
                val slotHeight = slot.weight * unitHeightPx
                val x = (constraints.maxWidth - placeable.width) / 2
                val yPos = y + (slotHeight - placeable.height) / 2
                placeable.place(x.toInt(), yPos.toInt())
                y += slotHeight
            }
        }
    }
}

@Composable
fun StaticGridBackground(unitHeightPx: Float) {
    val dashColor = Color(0xFFF5F5F5)
    val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        var y = 0f
        DailySchedule.forEach { slot ->
            val height = slot.weight * unitHeightPx
            if (slot.type == SlotType.CLASS) {
                drawLine(
                    color = dashColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = pathEffect
                )
            }
            y += height
        }
    }
}

@Composable
fun DynamicWeekContent(
    courses: List<CourseWithTimes>,
    weekStartDate: LocalDate,
    unitHeightPx: Float,
    maxWidth: Dp,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 日期行 (固定高度)
        DynamicDateRow(weekStartDate)

        // 2. 剩余空间 (网格高度)
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            // 今天高亮
            HighlightTodayColumn(weekStartDate, maxWidth)
            // 课程叠加
            ScheduleCourseOverlay(courses, unitHeightPx, maxWidth, onCourseClick)
        }
    }
}

@Composable
fun DynamicDateRow(startDate: LocalDate) {
    val today = remember { LocalDate.now() }
    // 关键修复：高度强制固定为 DateHeaderHeight
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(DateHeaderHeight)
        .padding(bottom = 4.dp)) {
        for (i in 0..6) {
            val date = startDate.plusDays(i.toLong())
            val isToday = date == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isToday) Color(0xFFE3F2FD) else Color.Transparent)
                    .fillMaxHeight(), // 填满固定高度
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${date.monthValue}/${date.dayOfMonth}",
                    fontSize = 11.sp,
                    color = if (isToday) Color(0xFF1565C0) else Color.Gray,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun HighlightTodayColumn(weekStartDate: LocalDate, maxWidth: Dp) {
    val today = LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(weekStartDate, today).toInt()

    if (daysBetween in 0..6) {
        val density = LocalDensity.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val colWidthPx = totalWidthPx / 7
            val xPx = colWidthPx * daysBetween

            drawRect(
                color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                topLeft = Offset(xPx, 0f),
                size = Size(colWidthPx, size.height)
            )
            drawLine(
                color = Color(0xFF64B5F6).copy(alpha = 0.5f),
                start = Offset(xPx, 0f),
                end = Offset(xPx, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFF64B5F6).copy(alpha = 0.5f),
                start = Offset(xPx + colWidthPx, 0f),
                end = Offset(xPx + colWidthPx, size.height),
                strokeWidth = 2f
            )
        }
    }
}

// 渲染数据类
private data class LayoutItem(
    val course: CourseWithTimes,
    val time: ClassTimeEntity,
    val startIndex: Int,
    val endIndex: Int,
    val dayIndex: Int
)

@Composable
fun ScheduleCourseOverlay(
    courses: List<CourseWithTimes>,
    unitHeightPx: Float,
    maxWidth: Dp,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
) {
    val density = LocalDensity.current

    val preparedItems = remember(courses) {
        val items = mutableListOf<LayoutItem>()
        courses.forEach { course ->
            course.times.forEach { time ->
                val dayIndex = parseWeekday(time.weekday) - 1
                if (dayIndex in 0..6) {
                    val (startPeriod, span) = parsePeriod(time.period)
                    val startIndex = SectionIndexMap[startPeriod.toString()] ?: -1
                    if (startIndex != -1) {
                        var spanCounter = 0
                        var endIndex = startIndex
                        while (spanCounter < span && endIndex < DailySchedule.size) {
                            if (DailySchedule[endIndex].type == SlotType.CLASS) spanCounter++
                            endIndex++
                        }
                        items.add(LayoutItem(course, time, startIndex, endIndex, dayIndex))
                    }
                }
            }
        }
        items.groupBy { it.dayIndex }
    }

    Layout(
        content = {
            for (day in 0..6) {
                val dayItems = preparedItems[day] ?: emptyList()
                val groups = dayItems.groupBy { "${it.startIndex}-${it.endIndex}" }

                groups.forEach { (_, groupItems) ->
                    val distinctNames = groupItems.map { it.course.course.courseName }.distinct()
                    val hasDifferentCourses = distinctNames.size > 1
                    val overlappedData = groupItems.map { it.course to it.time }

                    if (hasDifferentCourses) {
                        groupItems.forEachIndexed { i, item ->
                            val baseColor =
                                CourseColors[kotlin.math.abs(item.course.course.courseName.hashCode()) % CourseColors.size]
                            CourseCard(
                                title = item.course.course.courseName,
                                location = item.time.location,
                                color = baseColor.copy(alpha = 0.85f),
                                onClick = { onCourseClick(overlappedData) },
                                modifier = Modifier.layoutId(item)
                            )
                        }
                    } else if (groupItems.isNotEmpty()) {
                        val item = groupItems.first()
                        val baseColor =
                            CourseColors[kotlin.math.abs(item.course.course.courseName.hashCode()) % CourseColors.size]
                        CourseCard(
                            title = item.course.course.courseName,
                            location = item.time.location,
                            color = baseColor,
                            onClick = { onCourseClick(overlappedData) },
                            modifier = Modifier.layoutId(item)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val totalWidthPx = constraints.maxWidth.toFloat()
        val colWidthPx = totalWidthPx / 7f

        val slotYPositions = FloatArray(DailySchedule.size + 1)
        var currentY = 0f
        DailySchedule.forEachIndexed { index, slot ->
            slotYPositions[index] = currentY
            currentY += slot.weight * unitHeightPx
        }
        slotYPositions[DailySchedule.size] = currentY

        val placeables = measurables.map { measurable ->
            val item = measurable.layoutId as LayoutItem
            val yPos = slotYPositions[item.startIndex]
            val endYPos = slotYPositions[item.endIndex.coerceAtMost(DailySchedule.size)]
            val height = (endYPos - yPos).roundToInt() - 2.dp.roundToPx()
            val width = colWidthPx.roundToInt() - 2.dp.roundToPx()

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
                val dayIndex = item.dayIndex
                val xPos = (colWidthPx * dayIndex).roundToInt()
                placeable.place(xPos, yPos.roundToInt())
            }
        }
    }
}

// ... 保持 AccountSelectionDialog, LoginDialog, CourseDetailContent, DetailItem, CourseCard, parseWeekday, parsePeriod, CourseRenderInfo 不变 ...
// (为了节省篇幅，假设下方包含这些函数，请直接使用上一次回答中的相应部分)

@Composable
private fun CourseCard(
    title: String,
    location: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@$location",
                    fontSize = 9.sp,
                    color = Color.White.copy(0.95f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun parseWeekday(day: String): Int = when {
    day.contains("一") || day == "1" -> 1
    day.contains("二") || day == "2" -> 2
    day.contains("三") || day == "3" -> 3
    day.contains("四") || day == "4" -> 4
    day.contains("五") || day == "5" -> 5
    day.contains("六") || day == "6" -> 6
    day.contains("日") || day == "7" -> 7
    else -> 1
}

fun parsePeriod(period: String): Pair<Int, Int> {
    try {
        val clean = period.replace("节", "")
        val parts = clean.split("-")
        if (parts.size == 2) return parts[0].toInt() to (parts[1].toInt() - parts[0].toInt() + 1)
        clean.toIntOrNull()?.let { return it to 1 }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return 1 to 2
}

@Composable
fun AccountSelectionDialog(
    accounts: Map<String, String>,
    currentId: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("切换用户") },
        text = {
            LazyColumn {
                items(accounts.keys.toList()) { id ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = id == currentId, onClick = { onSelect(id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("学号: $id", fontSize = 16.sp)
                    }
                }
                item {
                    TextButton(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null
                        ); Spacer(modifier = Modifier.width(8.dp)); Text("添加新账号")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun CourseDetailContent(
    infoList: List<Pair<CourseWithTimes, ClassTimeEntity>>,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = if (infoList.size > 1) "课程详情 (${infoList.size})" else "课程详情"
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (infoList.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { infoList.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                pageSpacing = 16.dp
            ) { page ->
                val (courseData, timeData) = infoList[page]
                CourseDetailCard(courseData, timeData)
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
                        val color =
                            if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
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
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CourseDetailCard(courseData: CourseWithTimes, timeData: ClassTimeEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailItem(Icons.Default.Book, "课程名称", courseData.course.courseName)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
            DetailItem(Icons.Default.Place, "上课地点", timeData.location)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
            DetailItem(Icons.Default.Person, "教师", timeData.teacher)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
            DetailItem(Icons.Default.AccessTime, "时间", "${timeData.weekday} ${timeData.period}节")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
            DetailItem(Icons.Default.DateRange, "周次", timeData.weeks)
            if (courseData.course.category.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
                DetailItem(Icons.Default.Category, "类型", courseData.course.category)
            }
        }
    }
}

@Composable
fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF757575))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                fontSize = 10.sp,
                color = Color.Gray
            ); Text(if (value.isBlank()) "无" else value, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun LoginDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("导入课表") },
        text = {
            Column {
                OutlinedTextField(
                    u,
                    { u = it },
                    label = { Text("学号") },
                    singleLine = true
                ); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(
                p,
                { p = it },
                label = { Text("密码") },
                singleLine = true
            )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(u, p) }) { Text("确定") } }
    )
}