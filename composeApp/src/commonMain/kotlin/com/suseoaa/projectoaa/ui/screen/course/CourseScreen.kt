package com.suseoaa.projectoaa.ui.screen.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.course.CourseViewModel
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

// 课程颜色列表
private val CourseColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFFFFCA28), Color(0xFF9CCC65), Color(0xFF7E57C2), Color(0xFF29B6F6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    viewModel: CourseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "课表",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.currentAccount != null) {
                                Text(
                                    "${uiState.currentAccount?.name} - ${uiState.currentAccount?.className}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${uiState.selectedXnm}学年",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    // 周次选择器
                    WeekSelectorRow(
                        currentWeek = uiState.currentWeek,
                        totalWeeks = 25,
                        onWeekSelected = { week ->
                            scope.launch {
                                pagerState.animateScrollToPage(week - 1)
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: 添加自定义课程 */ },
                containerColor = ElectricBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程", tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.courses.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无课程数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkGrey
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请先登录教务系统同步课表",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkGrey.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* TODO: 登录教务 */ }) {
                        Text("同步课表")
                    }
                }
            } else {
                // 课程表 Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val weekNumber = page + 1
                    CourseTableView(
                        courses = uiState.courses,
                        currentWeek = weekNumber
                    )
                }
            }
        }
    }
}

@Composable
fun WeekSelectorRow(
    currentWeek: Int,
    totalWeeks: Int,
    onWeekSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalWeeks) { index ->
            val week = index + 1
            val isSelected = week == currentWeek
            
            Surface(
                onClick = { onWeekSelected(week) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) ElectricBlue else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$week",
                    textAlign = TextAlign.Center,
                    color = if (isSelected) Color.White else InkGrey,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CourseTableView(
    courses: List<com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes>,
    currentWeek: Int
) {
    // 星期头部
    val weekDays = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 星期头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            weekDays.forEachIndexed { index, day ->
                Box(
                    modifier = Modifier.weight(if (index == 0) 0.5f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkGrey
                    )
                }
            }
        }
        
        Divider(color = OutlineSoft)
        
        // 课程表内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 12节课
            items(12) { periodIndex ->
                val period = periodIndex + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    // 节次列
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$period",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkGrey
                        )
                    }
                    
                    // 周一到周日
                    repeat(7) { dayIndex ->
                        val weekday = dayIndex + 1
                        val coursesAtSlot = courses.filter { courseWithTimes ->
                            courseWithTimes.times.any { time ->
                                time.weekday == weekday.toString() &&
                                time.period.contains(period.toString()) &&
                                isWeekInRange(time.weeksMask, currentWeek)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp)
                        ) {
                            if (coursesAtSlot.isNotEmpty()) {
                                val course = coursesAtSlot.first()
                                val colorIndex = course.course.courseName.hashCode().mod(CourseColors.size).let { 
                                    if (it < 0) it + CourseColors.size else it 
                                }
                                
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = CourseColors[colorIndex].copy(alpha = 0.2f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(2.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = course.course.courseName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CourseColors[colorIndex],
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 9.sp,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (periodIndex < 11) {
                    Divider(color = OutlineSoft.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// 检查周次是否在范围内
fun isWeekInRange(weeksMask: Long, week: Int): Boolean {
    if (week < 1 || week > 64) return false
    return (weeksMask and (1L shl (week - 1))) != 0L
}
