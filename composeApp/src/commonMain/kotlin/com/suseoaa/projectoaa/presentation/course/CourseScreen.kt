package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.data.model.CourseAccountEntity
import com.suseoaa.projectoaa.data.model.CourseWithTimes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: CourseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val xnm by viewModel.selectedXnm.collectAsStateWithLifecycle()
    val xqm by viewModel.selectedXqm.collectAsStateWithLifecycle()
    val currentWeek by viewModel.currentDisplayWeek.collectAsStateWithLifecycle()
    
    // 监听自动登录或数据加载状态
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // Show snackbar? For now just log or let UI handle
        }
    }

    Scaffold(
        topBar = {
            CourseTopBar(
                title = if (currentAccount != null) "第${currentWeek}周" else "课程表",
                subTitle = if (currentAccount != null) "${xnm}-${xnm.toInt() + 1}学年 第${xqm}学期" else "未登录",
                accounts = savedAccounts,
                currentAccount = currentAccount,
                onAccountSelected = { viewModel.switchUser(it) },
                onAddAccount = onNavigateToLogin,
                onRefresh = { viewModel.refreshSchedule() },
                onTermClick = { /* TODO: Open Term Selector */ }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (currentAccount == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onNavigateToLogin) {
                        Text("添加教务账号")
                    }
                }
            } else {
                WeekSelector(
                    currentWeek = currentWeek,
                    onWeekSelected = { viewModel.setDisplayWeek(it) }
                )
                
                // 课表主体
                val allCourses by viewModel.weekScheduleMap.collectAsStateWithLifecycle()
                val coursesForWeek = allCourses[currentWeek] ?: emptyList()
                val dailySchedule by viewModel.dailySchedule.collectAsStateWithLifecycle()
                
                TimetableGrid(
                    courses = coursesForWeek,
                    timeSlots = dailySchedule
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTopBar(
    title: String,
    subTitle: String,
    accounts: List<CourseAccountEntity>,
    currentAccount: CourseAccountEntity?,
    onAccountSelected: (CourseAccountEntity) -> Unit,
    onAddAccount: () -> Unit,
    onRefresh: () -> Unit,
    onTermClick: () -> Unit
) {
    var showAccountMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(modifier = Modifier.clickable { onTermClick() }) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subTitle, style = MaterialTheme.typography.bodySmall)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
            Box {
                IconButton(onClick = { showAccountMenu = true }) {
                    // Show Avatar or Initials
                    if (currentAccount != null) {
                        Surface(
                             shape = MaterialTheme.shapes.small,
                             color = MaterialTheme.colorScheme.primaryContainer,
                             modifier = Modifier.size(32.dp)
                        ) {
                             Box(contentAlignment = Alignment.Center) {
                                 Text(
                                     text = currentAccount.name.take(1),
                                     style = MaterialTheme.typography.labelLarge
                                 )
                             }
                        }
                    } else {
                        Icon(Icons.Default.Add, "Add Account")
                    }
                }
                DropdownMenu(
                    expanded = showAccountMenu,
                    onDismissRequest = { showAccountMenu = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(account.name)
                                    Text(account.studentId, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                onAccountSelected(account)
                                showAccountMenu = false
                            },
                            trailingIcon = if (account.studentId == currentAccount?.studentId) {
                                { Text("当前", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("添加新账号") },
                        onClick = {
                            onAddAccount()
                            showAccountMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                }
            }
        }
    )
}

@Composable
fun WeekSelector(
    currentWeek: Int,
    onWeekSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(25) { index ->
            val week = index + 1
            FilterChip(
                selected = week == currentWeek,
                onClick = { onWeekSelected(week) },
                label = { Text("第${week}周") }
            )
        }
    }
}

@Composable
fun TimetableGrid(
    courses: List<CourseWithTimes>,
    timeSlots: List<TimeSlotConfig>
) {
    val scrollState = rememberScrollState()
    
    Row(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Left Time Column
        Column(modifier = Modifier.width(40.dp)) {
            timeSlots.forEach { slot ->
                if (slot.type == SlotType.CLASS) {
                     Box(
                         modifier = Modifier
                             .height(60.dp) // Fixed height per slot
                             .fillMaxWidth(),
                         contentAlignment = Alignment.Center
                     ) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text(slot.sectionName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                             Text(slot.startTime, fontSize = 10.sp, color = Color.Gray)
                             Text(slot.endTime, fontSize = 10.sp, color = Color.Gray)
                         }
                     }
                } else {
                     // Break slots
                     Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        // Main Grid
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 7 Days
                repeat(7) { dayIndex ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        var accumulatedHeight = 0.dp
                        
                        timeSlots.forEach { slot -> 
                             val slotHeight = if (slot.type == SlotType.CLASS) 60.dp else 30.dp
                             
                             // Find course in this slot for this day
                             val courseForSlot = courses.find { c ->
                                 c.times.any { t ->
                                     // "1" = Monday. dayIndex starts at 0 -> Monday = 1
                                     // Need robust matching. Assuming 'weekday' in DB is "1"-"7"
                                     val dbDay = t.weekday.toIntOrNull() ?: 1
                                     val slotStart = slot.sectionName.toIntOrNull()
                                     val periodStart = t.period.split("-").firstOrNull()?.toIntOrNull()
                                     val periodEnd = t.period.split("-").lastOrNull()?.toIntOrNull()
                                     
                                     if (slotStart != null && periodStart != null && periodEnd != null) {
                                         dbDay == (dayIndex + 1) && slotStart >= periodStart && slotStart <= periodEnd
                                     } else false
                                 }
                             }

                             if (courseForSlot != null && slot.type == SlotType.CLASS) {
                                 // Only draw if it's the START of the course to span multiple cells?
                                 // Simple impl: Use Box with offset.
                                 // But inside Column it's hard to span.
                                 // Better approach: Use Custom Layout or Box with absolute offsets.
                                 // For now: Just render content inside the cell.
                                 
                                 // Check if this slot is the START of the course period
                                 // This is a simplified rendering that might clip.
                                 
                                 Card(
                                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .height(slotHeight)
                                         .padding(1.dp)
                                 ) {
                                     Text(
                                         text = courseForSlot.course.courseName,
                                         style = MaterialTheme.typography.labelSmall,
                                         fontSize = 10.sp,
                                         lineHeight = 12.sp,
                                         overflow = TextOverflow.Ellipsis,
                                         modifier = Modifier.padding(2.dp)
                                     )
                                 }
                             } else {
                                 // Empty cell
                                 Spacer(modifier = Modifier.height(slotHeight))
                             }
                        }
                    }
                }
            }
        }
    }
}
