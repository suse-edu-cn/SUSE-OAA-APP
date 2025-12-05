package com.suseoaa.projectoaa.courseList.ui.screen

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.LocalWindowSizeClass
import com.suseoaa.projectoaa.courseList.data.entity.ClassTimeEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseAccountEntity
import com.suseoaa.projectoaa.courseList.data.entity.CourseWithTimes
import com.suseoaa.projectoaa.courseList.viewmodel.CourseListViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.String
import kotlin.math.roundToInt

// 课程卡片颜色
private val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6)
)

data class TimeSlotConfig(
    val sectionName: String,
    val startTime: String,
    val endTime: String,
    val type: SlotType,
    val weight: Float
)

enum class SlotType { CLASS, BREAK_SMALL, BREAK_LUNCH, BREAK_DINNER }

// 2025年及以后的作息 (11节课)
private val DailySchedulePost2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.2f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.2f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.2f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_DINNER, 0.4f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.2f)
)

// 2025年以前的旧作息 (12节课)
private val DailySchedulePre2025 = listOf(
    TimeSlotConfig("1", "08:30", "09:15", SlotType.CLASS, 1.2f),
    TimeSlotConfig("2", "09:20", "10:05", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("3", "10:25", "11:10", SlotType.CLASS, 1.2f),
    TimeSlotConfig("4", "11:15", "12:00", SlotType.CLASS, 1.2f),
    TimeSlotConfig("午餐", "12:00", "14:00", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("午休", "", "", SlotType.BREAK_LUNCH, 0.5f),
    TimeSlotConfig("5", "14:00", "14:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("6", "14:50", "15:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_SMALL, 0.2f),
    TimeSlotConfig("7", "15:55", "16:40", SlotType.CLASS, 1.2f),
    TimeSlotConfig("8", "16:45", "17:30", SlotType.CLASS, 1.2f),
    TimeSlotConfig("", "", "", SlotType.BREAK_DINNER, 0.4f),
    TimeSlotConfig("9", "19:00", "19:45", SlotType.CLASS, 1.2f),
    TimeSlotConfig("10", "19:50", "20:35", SlotType.CLASS, 1.2f),
    TimeSlotConfig("11", "20:40", "21:25", SlotType.CLASS, 1.2f),
    TimeSlotConfig("12", "21:30", "22:15", SlotType.CLASS, 1.2f)
)

private fun getDailySchedule(yearStr: String): List<TimeSlotConfig> {
    val year = yearStr.toIntOrNull() ?: 2025
    return if (year >= 2025) DailySchedulePost2025 else DailySchedulePre2025
}

private val DateHeaderHeight = 32.dp

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: CourseListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val context = LocalContext.current

    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val startDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val termOptions by viewModel.termOptions.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val currentDailySchedule = remember(viewModel.selectedXnm) {
        getDailySchedule(viewModel.selectedXnm)
    }

    var selectedCourses by remember {
        mutableStateOf<List<Pair<CourseWithTimes, ClassTimeEntity>>?>(
            null
        )
    }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCustomCourseDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = (viewModel.currentDisplayWeek - 1).coerceAtLeast(0),
        pageCount = { 25 }
    )

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
        if (pagerState.currentPage != targetPage && targetPage in 0..24 && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    val datePickerDialog = remember {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                viewModel.setSemesterStartDate(LocalDate.of(y, m + 1, d))
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 2.dp,
                modifier = Modifier.zIndex(1f)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                when (windowSizeClass.widthSizeClass) {
                                    WindowWidthSizeClass.Compact -> Modifier.padding(8.dp)
                                    WindowWidthSizeClass.Expanded -> Modifier.statusBarsPadding()
                                    WindowWidthSizeClass.Medium -> Modifier.statusBarsPadding()
                                    else -> Modifier.statusBarsPadding()
                                }
                            )
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { termDropdownExpanded = true }
                                    .padding(8.dp)
                            ) {
                                val currentLabel = termOptions.find {
                                    it.xnm == viewModel.selectedXnm && it.xqm == viewModel.selectedXqm
                                }?.label ?: "${viewModel.selectedXnm}学年"

                                Text(
                                    currentLabel,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = termDropdownExpanded,
                                onDismissRequest = { termDropdownExpanded = false }
                            ) {
                                termOptions.forEach { option ->
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
                                    onClick = { menuExpanded = false; viewModel.refreshSchedule() })
                                DropdownMenuItem(
                                    text = { Text("导入新课表") },
                                    onClick = { menuExpanded = false; showLoginDialog = true })
                                DropdownMenuItem(
                                    text = { Text("添加自定义课程") },
                                    onClick = {
                                        menuExpanded = false; showCustomCourseDialog = true
                                    })
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
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier
                                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                                    height = 4.dp, color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        for (w in 1..25) {
                            val isSelected = w == (pagerState.currentPage + 1)
                            val isRealCurrentWeek = w == viewModel.realCurrentWeek
                            val textColor =
                                if (isRealCurrentWeek) MaterialTheme.colorScheme.tertiary else if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
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
                            dailySchedule = currentDailySchedule,
                            onCourseClick = { selectedCourses = it }
                        )
                    }
                    if (isTablet && selectedCourses != null) {
                        Box(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            CourseDetailContent(selectedCourses!!) { selectedCourses = null }
                        }
                    }
                }
            }

            if (!isTablet && selectedCourses != null) {
                // [修复] 添加 skipPartiallyExpanded = true，确保打开时直接显示全部内容，不需要再上滑
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { selectedCourses = null },
                    containerColor = MaterialTheme.colorScheme.surface,
                    sheetState = sheetState
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

        if (showLoginDialog) LoginDialog({ showLoginDialog = false }) { u, p ->
            viewModel.fetchAndSaveCourseSchedule(u, p)
            showLoginDialog = false
        }

        if (showAccountDialog) AccountSelectionDialog(
            accounts = savedAccounts,
            currentId = currentAccount?.studentId ?: "",
            onSelect = { viewModel.switchUser(it); showAccountDialog = false },
            onDelete = { viewModel.deleteAccount(it) },
            onAdd = { showAccountDialog = false; showLoginDialog = true },
            onDismiss = { showAccountDialog = false }
        )

        if (showCustomCourseDialog) AddCustomCourseDialog(
            onDismiss = { showCustomCourseDialog = false },
            onConfirm = { name, loc, tea, day, start, dur, wks ->
                viewModel.addCustomCourse(name, loc, tea, day, start, dur, wks)
                showCustomCourseDialog = false
            }
        )
    }
}

@Composable
fun CourseScheduleLayout(
    allCourses: List<CourseWithTimes>,
    startDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    getCoursesForWeek: (Int, List<CourseWithTimes>) -> List<CourseWithTimes>,
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
) {
    val density = LocalDensity.current
    val timeAxisWidth = 40.dp

    Column(modifier = Modifier.fillMaxSize()) {
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
            val totalWeight =
                remember(dailySchedule) { dailySchedule.sumOf { it.weight.toDouble() }.toFloat() }
            val unitHeightPx = with(density) { gridHeight.toPx() } / totalWeight
            val parentMaxWidth = maxWidth

            Row(modifier = Modifier.fillMaxSize()) {
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

                Box(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(DateHeaderHeight))
                        StaticGridBackground(dailySchedule, unitHeightPx)
                    }

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

// CourseDetailContent
@Composable
fun CourseDetailContent(
    infoList: List<Pair<CourseWithTimes, ClassTimeEntity>>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                verticalAlignment = Alignment.Top
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
                            if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.3f
                            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            DetailItem(Icons.Default.Book, "课程名称", courseData.course.courseName)

            if (timeData.location.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                DetailItem(Icons.Default.Place, "上课地点", timeData.location)
            }
            if (timeData.teacher.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                DetailItem(Icons.Default.Person, "教师", timeData.teacher)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            DetailItem(Icons.Default.AccessTime, "时间", "${timeData.weekday} ${timeData.period}")

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            DetailItem(Icons.Default.DateRange, "周次", timeData.weeks)

            if (!courseData.course.isCustom) {
                if (courseData.course.assessment.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailItem(
                        Icons.AutoMirrored.Filled.Assignment,
                        "考察方式",
                        courseData.course.assessment
                    )
                }
                if (timeData.classGroup.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailItem(Icons.Default.Group, "上课班级", timeData.classGroup)
                }
                if (courseData.course.category.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailItem(Icons.Default.Category, "类型", courseData.course.category)
                }
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
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (value.isBlank()) "无" else value,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
        val placeables =
            measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = 100)) }
        layout(constraints.maxWidth, heightPx) {
            var y = 0f
            placeables.forEachIndexed { index, placeable ->
                val slot = dailySchedule[index]
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
fun DynamicWeekContent(
    courses: List<CourseWithTimes>,
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
            ScheduleCourseOverlay(courses, unitHeightPx, maxWidth, dailySchedule, onCourseClick)
        }
    }
}

@Composable
fun DynamicDateRow(startDate: LocalDate) {
    val today = remember { LocalDate.now() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DateHeaderHeight)
            .padding(bottom = 6.dp)
    ) {
        for (i in 0..6) {
            val date = startDate.plusDays(i.toLong())
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
                    text = "${date.monthValue}/${date.dayOfMonth}",
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
    val today = LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(weekStartDate, today).toInt()
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
    dailySchedule: List<TimeSlotConfig>,
    onCourseClick: (List<Pair<CourseWithTimes, ClassTimeEntity>>) -> Unit
) {
    val density = LocalDensity.current
    val sectionIndexMap = remember(dailySchedule) {
        dailySchedule.mapIndexedNotNull { index, slot ->
            if (slot.sectionName.isNotEmpty()) slot.sectionName to index else null
        }.toMap()
    }

    val preparedItems = remember(courses, sectionIndexMap) {
        val items = mutableListOf<LayoutItem>()
        courses.forEach { course ->
            course.times.forEach { time ->
                val dayIndex = parseWeekday(time.weekday) - 1
                if (dayIndex in 0..6) {
                    val (startPeriod, span) = parsePeriod(time.period)
                    val startIndex = sectionIndexMap[startPeriod.toString()] ?: -1
                    if (startIndex != -1) {
                        var spanCounter = 0
                        var endIndex = startIndex
                        while (spanCounter < span && endIndex < dailySchedule.size) {
                            if (dailySchedule[endIndex].type == SlotType.CLASS) spanCounter++
                            endIndex++
                        }
                        items.add(LayoutItem(course, time, startIndex, endIndex, dayIndex))
                    }
                }
            }
        }
        items.groupBy { it.dayIndex }
    }

    Layout(content = {
        for (day in 0..6) {
            val dayItems = preparedItems[day] ?: emptyList()
            val groups = dayItems.groupBy { "${it.startIndex}-${it.endIndex}" }
            groups.forEach { (_, groupItems) ->
                val overlappedData = groupItems.map { it.course to it.time }
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
            val item = measurable.layoutId as LayoutItem
            val yPos = slotYPositions[item.startIndex]
            val endYPos = slotYPositions[item.endIndex.coerceAtMost(dailySchedule.size)]
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

// ... (CourseCard, AccountSelectionDialog, LoginDialog, AddCustomCourseDialog, parseWeekday, parsePeriod 保持不变) ...
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
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.clickable { onClick() }) {
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
        containerColor = Color(0xFFFFFFFF),
        tonalElevation = 6.dp,
        onDismissRequest = onDismiss,
        title = { Text("切换用户") },
        text = {
            LazyColumn {
                items(accounts) { acc ->
                    var showPassword by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(acc) },
                        colors = if (acc.studentId == currentId) CardDefaults.cardColors(
                            containerColor = Color(0xD7FFFFFF)
                        ) else CardDefaults.cardColors()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${acc.name} - ${acc.className}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onDelete(acc) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Row(modifier = Modifier.clickable { showPassword = !showPassword }) {
                                Text(
                                    "学号: ${acc.studentId}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "密码: ${if (showPassword) acc.password else "******"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
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
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun LoginDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    AlertDialog(
        containerColor = Color(0xFFFFFFFF),
        onDismissRequest = onDismiss,
        title = { Text("导入课表") },
        text = {
            Column {
                OutlinedTextField(u, { u = it }, label = { Text("学号") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(p, { p = it }, label = { Text("密码") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(u, p) }) { Text("确定") } }
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
    var dayOfWeek by remember { mutableIntStateOf(1) } // 1-7
    var startNode by remember { mutableIntStateOf(1) } // 1-11
    var duration by remember { mutableIntStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(color = Color(0xFFFFFFFF))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("添加自定义课程", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 (必填)") })
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点 (选填)") })
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师 (选填)") })
                OutlinedTextField(
                    value = weeks,
                    onValueChange = { weeks = it },
                    label = { Text("周次 (例如 1-16)") })

                Text("星期: 周$dayOfWeek", Modifier.padding(top = 8.dp))
                Slider(
                    value = dayOfWeek.toFloat(),
                    onValueChange = { dayOfWeek = it.toInt() },
                    valueRange = 1f..7f,
                    steps = 5
                )

                Text("开始节次: 第${startNode}节")
                Slider(
                    value = startNode.toFloat(),
                    onValueChange = { startNode = it.toInt() },
                    valueRange = 1f..11f,
                    steps = 9
                )

                Text("持续节数: ${duration}节")
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(
                                name,
                                location,
                                teacher,
                                dayOfWeek,
                                startNode,
                                duration,
                                weeks
                            )
                        }
                    }) { Text("确定") }
                }
            }
        }
    }
}

fun parseWeekday(day: String): Int = when {
    day.contains("一") || day == "1" -> 1; day.contains("二") || day == "2" -> 2; day.contains("三") || day == "3" -> 3; day.contains(
        "四"
    ) || day == "4" -> 4; day.contains("五") || day == "5" -> 5; day.contains("六") || day == "6" -> 6; day.contains(
        "日"
    ) || day == "7" -> 7; else -> 1
}

fun parsePeriod(period: String): Pair<Int, Int> {
    try {
        val clean = period.replace("节", "");
        val parts =
            clean.split("-"); if (parts.size == 2) return parts[0].toInt() to (parts[1].toInt() - parts[0].toInt() + 1); clean.toIntOrNull()
            ?.let { return it to 1 }
    } catch (e: Exception) {
        e.printStackTrace()
    }; return 1 to 2
}