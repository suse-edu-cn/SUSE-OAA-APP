package com.suseoaa.projectoaa.presentation.course

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.data.model.ClassTimeEntity
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.model.CourseWithTimes
import com.suseoaa.projectoaa.util.ToastManager
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

// ==================== 课程颜色配置 ====================
private val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6)
)

private val DateHeaderHeight = 32.dp

// ==================== 主界面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    onNavigateToLogin: () -> Unit = {},
    viewModel: CourseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val xnm by viewModel.selectedXnm.collectAsStateWithLifecycle()
    val xqm by viewModel.selectedXqm.collectAsStateWithLifecycle()
    val currentDisplayWeek by viewModel.currentDisplayWeek.collectAsStateWithLifecycle()
    val realCurrentWeek by viewModel.realCurrentWeek.collectAsStateWithLifecycle()
    val termOptions by viewModel.termOptions.collectAsStateWithLifecycle()
    val weekLayoutMap by viewModel.weekLayoutMap.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val dailySchedule by viewModel.dailySchedule.collectAsStateWithLifecycle()
    val semesterStartDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    
    // 对话框状态
    var selectedCourses by remember { mutableStateOf<List<Pair<CourseWithTimes, ClassTimeEntity>>?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCustomCourseDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }

    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = (currentDisplayWeek - 1).coerceAtLeast(0),
        pageCount = { 25 }
    )

    // 监听 UI 消息 - 使用 Toast
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            ToastManager.showError(it)
            viewModel.clearUiMessage()
        }
        uiState.successMessage?.let {
            ToastManager.showSuccess(it)
            showLoginDialog = false
            viewModel.clearUiMessage()
        }
    }

    // 监听 Pager 变化
    LaunchedEffect(pagerState.settledPage) {
        val newWeek = pagerState.settledPage + 1
        if (currentDisplayWeek != newWeek) {
            viewModel.setDisplayWeek(newWeek)
        }
    }

    // 监听 ViewModel 周次变化
    LaunchedEffect(currentDisplayWeek) {
        val targetPage = currentDisplayWeek - 1
        if (pagerState.currentPage != targetPage && targetPage in 0..24 && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 2.dp
            ) {
                Column {
                    // 顶部栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：标题和当前账号信息
                        Column {
                            Text(
                                "课表",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentAccount != null) {
                                Text(
                                    "${currentAccount?.name} - ${currentAccount?.className}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // 中间：学期选择器
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { termDropdownExpanded = true }
                            ) {
                                val currentLabel = termOptions.find {
                                    it.xnm == xnm && it.xqm == xqm
                                }?.label ?: "${xnm}学年"

                                Text(
                                    currentLabel,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
                                expanded = termDropdownExpanded,
                                onDismissRequest = { termDropdownExpanded = false }
                            ) {
                                termOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.selectTerm(option.xnm, option.xqm)
                                            termDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 右侧：更多菜单
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
                                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
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
                                    onClick = { menuExpanded = false; showCustomCourseDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("查看/切换账号") },
                                    onClick = { menuExpanded = false; showAccountDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置开学日期") },
                                    onClick = { menuExpanded = false; showDatePickerDialog = true }
                                )
                            }
                        }
                    }

                    // 周次选项卡
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = {
                            if (pagerState.currentPage < 25) {
                                SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(pagerState.currentPage)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
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
                                onClick = { scope.launch { pagerState.animateScrollToPage(w - 1) } },
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
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (allCourses.isEmpty() && !uiState.isLoading) {
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
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showLoginDialog = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("导入课表")
                        }
                    }
                }
            } else {
                // 课表主体
                CourseScheduleLayout(
                    weekLayoutMap = weekLayoutMap,
                    startDate = semesterStartDate,
                    pagerState = pagerState,
                    dailySchedule = dailySchedule,
                    onCourseClick = { selectedCourses = it }
                )
            }

            // 加载指示器
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        // ==================== 对话框 ====================
        
        // 课程详情对话框
        selectedCourses?.let { courses ->
            Dialog(onDismissRequest = { selectedCourses = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    CourseDetailContent(
                        infoList = courses,
                        onClose = { selectedCourses = null },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 登录导入对话框
        if (showLoginDialog) {
            LoginDialog(
                isLoading = uiState.isLoading,
                onDismiss = { showLoginDialog = false },
                onConfirm = { u, p ->
                    viewModel.fetchAndSaveCourseSchedule(u, p)
                }
            )
        }

        // 账号管理对话框
        if (showAccountDialog) {
            AccountSelectionDialog(
                accounts = savedAccounts,
                currentId = currentAccount?.studentId ?: "",
                onSelect = { viewModel.switchUser(it); showAccountDialog = false },
                onDelete = { viewModel.deleteAccount(it) },
                onAdd = { showAccountDialog = false; showLoginDialog = true },
                onDismiss = { showAccountDialog = false }
            )
        }

        // 添加自定义课程对话框
        if (showCustomCourseDialog) {
            AddCustomCourseDialog(
                onDismiss = { showCustomCourseDialog = false },
                onConfirm = { name, loc, tea, day, start, dur, wks ->
                    viewModel.addCustomCourse(name, loc, tea, wks, day, start, dur)
                    showCustomCourseDialog = false
                }
            )
        }

        // 开学日期选择器
        if (showDatePickerDialog) {
            SemesterStartDatePicker(
                currentDate = semesterStartDate,
                onDateSelected = { viewModel.setSemesterStartDate(it) },
                onDismiss = { showDatePickerDialog = false }
            )
        }
    }
}

// ==================== 课表布局组件 ====================

@Composable
fun CourseScheduleLayout(
    weekLayoutMap: Map<Int, List<ScheduleLayoutItem>>,
    startDate: LocalDate,
    pagerState: PagerState,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
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
                        val weekIndex = page + 1
                        val weekStart = remember(startDate, page) {
                            startDate.plus(page * 7, DateTimeUnit.DAY)
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
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
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

@Composable
fun ScheduleCourseOverlay(
    items: List<ScheduleLayoutItem>,
    unitHeightPx: Float,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
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
                val overlappedData = groupItems.map { it.course to it.time }
                val item = groupItems.first()
                val courseName = item.course.course.courseName
                val index = (courseName.hashCode() and Int.MAX_VALUE) % CourseColors.size
                val baseColor = CourseColors[index]

                CourseCard(
                    title = courseName,
                    location = item.time.location,
                    color = baseColor,
                    onClick = { onCourseClick(overlappedData) },
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
            val endYPos = slotYPositions[(item.endNodeIndex + 1).coerceAtMost(dailySchedule.size)]
            val height = (endYPos - yPos).roundToInt() - 2.dp.roundToPx()
            val width = colWidthPx.roundToInt() - 2.dp.roundToPx()
            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    width = width.coerceAtLeast(0), height = height.coerceAtLeast(0)
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
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (location.isNotBlank()) {
                val displayLocation = location.removePrefix("L")
                Text(
                    text = displayLocation,
                    fontSize = 9.sp,
                    color = Color.White.copy(0.9f),
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==================== 静态组件 ====================

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

@Composable
fun StaticTimeAxis(dailySchedule: List<TimeSlotConfig>, unitHeightPx: Float, height: Dp) {
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
                            lineHeight = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else if (slot.type == SlotType.BREAK_LUNCH) {
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
        val heightPx = height.roundToPx()
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
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = pathEffect
                )
            }
            y += height
        }
    }
}

@Composable
fun DynamicDateRow(startDate: LocalDate) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
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
                    fontSize = 11.sp,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun HighlightTodayColumn(weekStartDate: LocalDate, maxWidth: Dp) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
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

// ==================== 课程详情组件 ====================

@Composable
fun CourseDetailContent(
    infoList: List<Pair<CourseWithTimes, ClassTimeEntity>>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
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

@Composable
fun CourseDetailCard(
    courseData: CourseWithTimes,
    timeData: ClassTimeEntity
) {
    val details = remember(courseData, timeData) {
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
                    add(DetailInfo(Icons.Default.Person, "上课班级", timeData.classGroup.replace(";", "\n")))
                }
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

// ==================== 对话框组件 ====================

@Composable
fun AccountSelectionDialog(
    accounts: List<CourseAccountEntity>,
    currentId: String,
    onSelect: (CourseAccountEntity) -> Unit,
    onDelete: (CourseAccountEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 6.dp,
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "教务系统账号管理",
                style = MaterialTheme.typography.titleMedium
            ) 
        },
        text = {
            Column {
                Text(
                    "这些是您保存的教务系统账号，用于导入课表。\n与软件登录账号无关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn {
                    items(accounts) { acc ->
                        var showPassword by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(acc) },
                            colors = if (acc.studentId == currentId) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${acc.name} - ${acc.className}", fontWeight = FontWeight.Bold)
                                        if (acc.studentId == currentId) {
                                            Text(
                                                "当前选中",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDelete(acc) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            "删除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.clickable { showPassword = !showPassword }
                                ) {
                                    Text(
                                        "学号: ${acc.studentId}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "密码: ${if (showPassword) acc.password else "******"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("添加新账号")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun LoginDialog(
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text("导入课表") },
        text = {
            Column {
                Text(
                    "请输入教务系统账号（学号和密码）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("教务系统密码") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, password) },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
fun AddCustomCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var weeks by remember { mutableStateOf("1-16") }
    var dayOfWeek by remember { mutableStateOf(1f) }
    var startNode by remember { mutableStateOf(1f) }
    var duration by remember { mutableStateOf(2f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("添加自定义课程", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 (必填)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点 (选填)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师 (选填)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weeks,
                    onValueChange = { weeks = it },
                    label = { Text("周次 (例如 1-16)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Text("星期: 周${dayOfWeek.roundToInt()}", Modifier.padding(top = 8.dp))
                Slider(
                    value = dayOfWeek,
                    onValueChange = { dayOfWeek = it },
                    valueRange = 1f..7f,
                    steps = 5
                )

                Text("开始节次: 第${startNode.roundToInt()}节")
                Slider(
                    value = startNode,
                    onValueChange = { startNode = it },
                    valueRange = 1f..11f,
                    steps = 9
                )

                Text("持续节数: ${duration.roundToInt()}节")
                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = 1f..4f,
                    steps = 2
                )

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    name,
                                    location,
                                    teacher,
                                    dayOfWeek.roundToInt(),
                                    startNode.roundToInt(),
                                    duration.roundToInt(),
                                    weeks
                                )
                            }
                        },
                        enabled = name.isNotBlank()
                    ) { Text("确定") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterStartDatePicker(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDate.toEpochDays() * 24L * 60L * 60L * 1000L
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val epochDays = (millis / (24L * 60L * 60L * 1000L)).toInt()
                        val date = LocalDate.fromEpochDays(epochDays)
                        onDateSelected(date)
                    }
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    "选择开学日期",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    "选择本学期第一周的周一",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    }
}
