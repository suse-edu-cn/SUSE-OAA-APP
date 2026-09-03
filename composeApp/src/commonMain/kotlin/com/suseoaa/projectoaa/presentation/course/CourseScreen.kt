package com.suseoaa.projectoaa.presentation.course

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.util.ToastManager
import com.suseoaa.projectoaa.util.pickImageForAvatar
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel


// ==================== 主界面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToCourseStatistics: () -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
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
    val overlapStatusByWeek by viewModel.overlapStatusByWeek.collectAsStateWithLifecycle()
    val overlapDetailByWeek by viewModel.overlapDetailByWeek.collectAsStateWithLifecycle()
    val overlapSelectedAccountIds by viewModel.overlapSelectedAccountIds.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val activeQueryCount by viewModel.activeQueryCount.collectAsStateWithLifecycle()
    val dailySchedule by viewModel.dailySchedule.collectAsStateWithLifecycle()
    val semesterStartDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()
    val hasWeekZero by viewModel.hasWeekZero.collectAsStateWithLifecycle()
    val courseBackgroundImageBase64 by viewModel.courseBackgroundImageBase64.collectAsStateWithLifecycle()
    val isMainTabVisible = LocalMainTabVisible.current

    // 动态周次范围
    val minWeek = if (hasWeekZero) 0 else 1
    val maxWeek = 25
    val totalWeeks = maxWeek - minWeek + 1

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncCurrentWeek()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 保活模式下，切回课程页时主动同步周次。
    LaunchedEffect(isMainTabVisible) {
        if (isMainTabVisible) {
            viewModel.syncCurrentWeek()
        }
    }

    // 对话框状态
    var selectedCourses by remember { mutableStateOf<CourseDetailSelection?>(null) }
    // 记录点击位置用于动画
    var clickedCardBounds by remember { mutableStateOf<Rect?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCustomCourseDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTermSelectionDialog by remember { mutableStateOf(false) }
    var showOverlapAccountDialog by remember { mutableStateOf(false) }
    var showCourseBackgroundPicker by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var overlapFilter by remember { mutableStateOf(OverlapDisplayFilter.ALL) }
    var onlyShowOverlap by remember { mutableStateOf(false) }
    val hasCourseBackgroundImage = !courseBackgroundImageBase64.isNullOrBlank()
    val haptic = LocalHapticFeedback.current

    // 双指缩放状态
    var courseScreenScale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = courseScreenScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "courseScreenScale"
    )

    val currentWeekItems = weekLayoutMap[currentDisplayWeek].orEmpty()
    val currentWeekOverlapMap = overlapStatusByWeek[currentDisplayWeek].orEmpty()
    val currentWeekOverlapDetailMap = overlapDetailByWeek[currentDisplayWeek].orEmpty()
    val overlapLegendCount = remember(currentWeekItems, currentWeekOverlapMap, activeQueryCount) {
        var noOverlap = 0
        var overlap = 0
        var partialOverlap = 0

        if (activeQueryCount > 1) {
            val prepared = buildPreparedCardItems(currentWeekItems, activeQueryCount)
            prepared.forEach { card ->
                when (card.overlapStatus) {
                    CourseOverlapStatus.NO_OVERLAP -> noOverlap++
                    CourseOverlapStatus.OVERLAP -> overlap++
                    CourseOverlapStatus.PARTIAL_OVERLAP -> partialOverlap++
                }
            }
        } else {
            currentWeekItems.forEach { item ->
                val status = currentWeekOverlapMap[buildScheduleLayoutOverlapKey(item)]
                    ?: CourseOverlapStatus.NO_OVERLAP
                when (status) {
                    CourseOverlapStatus.NO_OVERLAP -> noOverlap++
                    CourseOverlapStatus.OVERLAP -> overlap++
                    CourseOverlapStatus.PARTIAL_OVERLAP -> partialOverlap++
                }
            }
        }

        OverlapLegendCount(
            total = if (activeQueryCount > 1) (noOverlap + overlap + partialOverlap) else currentWeekItems.size,
            noOverlap = noOverlap,
            overlap = overlap,
            partialOverlap = partialOverlap
        )
    }

    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = (currentDisplayWeek - minWeek).coerceAtLeast(0),
        pageCount = { totalWeeks }
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
        val newWeek = pagerState.settledPage + minWeek
        if (currentDisplayWeek != newWeek) {
            viewModel.setDisplayWeek(newWeek)
        }
    }

    // 监听 ViewModel 周次变化
    LaunchedEffect(currentDisplayWeek, minWeek) {
        val targetPage = currentDisplayWeek - minWeek
        if (pagerState.currentPage != targetPage && targetPage in 0 until totalWeeks && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    if (showCourseBackgroundPicker) {
        pickImageForAvatar { imageData ->
            if (imageData != null) {
                viewModel.saveCourseBackgroundImage(imageData)
            }
            showCourseBackgroundPicker = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var triggered = false
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) {
                            courseScreenScale *= zoom
                            if (courseScreenScale < 0.85f && !triggered) {
                                triggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToCourseStatistics()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    courseScreenScale = 1f
                }
            },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧：标题和当前账号信息（固定宽度）
                        Column(modifier = Modifier.width(80.dp)) {
                            Text(
                                "课表",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentAccount != null) {
                                Text(
                                    "${currentAccount?.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 中间：学期选择器（自适应宽度，完整显示）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { showTermSelectionDialog = true }
                                .padding(horizontal = 8.dp)
                        ) {
                            val currentLabel = termOptions.find {
                                it.xnm == xnm && it.xqm == xqm
                            }?.label ?: "${xnm}学年"

                            Text(
                                currentLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                        }

                        // 右侧：更多菜单（固定宽度）
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    onClick = {
                                        menuExpanded = false; showCustomCourseDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("查看/切换账号") },
                                    onClick = { menuExpanded = false; showAccountDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("重课查询设置") },
                                    onClick = {
                                        menuExpanded = false
                                        showOverlapAccountDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置开学日期") },
                                    onClick = { menuExpanded = false; showDatePickerDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置课表背景图") },
                                    onClick = {
                                        menuExpanded = false
                                        showCourseBackgroundPicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("清除课表背景图") },
                                    enabled = hasCourseBackgroundImage,
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.clearCourseBackgroundImage()
                                    }
                                )
                            }
                        }
                    }

                    // 周次选项卡
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = {
                            if (pagerState.currentPage < totalWeeks) {
                                SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(pagerState.currentPage)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.height(32.dp)  // 限制 TabRow 高度
                    ) {
                        for (w in minWeek..maxWeek) {
                            val isSelected = w == (pagerState.currentPage + minWeek)
                            val isRealCurrentWeek = w == realCurrentWeek
                            val textColor = when {
                                isRealCurrentWeek -> MaterialTheme.colorScheme.tertiary
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Tab(
                                selected = isSelected,
                                onClick = { scope.launch { pagerState.animateScrollToPage(w - minWeek) } },
                                text = {
                                    Text(
                                        "${w}周",
                                        fontSize = if (isSelected) 14.sp else 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                },
                                modifier = Modifier.height(28.dp)
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
                        if (currentAccount != null) {
                            // 有账号但没课程，显示提示（会自动获取）
                            Text(
                                "本学期暂无课程",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "可尝试切换学期或刷新",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            // 没有账号，显示导入按钮
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
                }
            } else {
                // 课表主体
                val accountNameById = remember(savedAccounts) {
                    savedAccounts.associate { it.studentId to (it.name.ifBlank { it.studentId }) }
                }
                CourseScheduleLayout(
                    weekLayoutMap = weekLayoutMap,
                    overlapStatusByWeek = overlapStatusByWeek,
                    overlapFilter = overlapFilter,
                    onlyShowOverlap = onlyShowOverlap,
                    activeQueryCount = activeQueryCount,
                    accountNameById = accountNameById,
                    startDate = semesterStartDate,
                    pagerState = pagerState,
                    dailySchedule = dailySchedule,
                    minWeek = minWeek,
                    bottomPadding = bottomBarHeight,
                    onCourseClick = { courses, bounds ->
                        clickedCardBounds = bounds
                        selectedCourses = CourseDetailSelection(
                            items = courses,
                            overlapDetailByKey = courses.associate { item ->
                                val key = buildScheduleLayoutOverlapKey(item)
                                key to (currentWeekOverlapDetailMap[key]
                                    ?: CourseOverlapDetail(status = CourseOverlapStatus.NO_OVERLAP))
                            }
                        )
                    }
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

        // 课程详情对话框（带缩放动画）
        selectedCourses?.let { courses ->
            ScaleAnimatedDialog(
                onDismissRequest = {
                    selectedCourses = null
                    clickedCardBounds = null
                },
                originBounds = clickedCardBounds
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    val accountNameById = remember(savedAccounts) {
                        savedAccounts.associate { it.studentId to (it.name.ifBlank { it.studentId }) }
                    }
                    CourseDetailContent(
                        infoList = courses.items,
                        overlapDetailByKey = courses.overlapDetailByKey,
                        activeQueryCount = activeQueryCount,
                        accountNameById = accountNameById,
                        onClose = {
                            selectedCourses = null
                            clickedCardBounds = null
                        },
                        onDelete = { courseName, studentId ->
                            if (studentId == currentAccount?.studentId) {
                                viewModel.deleteCourse(courseName)
                            }
                            selectedCourses = null
                            clickedCardBounds = null
                        },
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

        if (showOverlapAccountDialog) {
            OverlapAccountSelectionDialog(
                accounts = savedAccounts,
                currentId = currentAccount?.studentId.orEmpty(),
                selectedIds = overlapSelectedAccountIds,
                count = overlapLegendCount,
                selectedFilter = overlapFilter,
                onlyShowOverlap = onlyShowOverlap,
                onFilterSelected = {
                    overlapFilter = it
                    if (onlyShowOverlap && it == OverlapDisplayFilter.NO_OVERLAP) {
                        onlyShowOverlap = false
                    }
                },
                onOnlyShowOverlapChange = { enabled ->
                    onlyShowOverlap = enabled
                    if (enabled && overlapFilter == OverlapDisplayFilter.NO_OVERLAP) {
                        overlapFilter = OverlapDisplayFilter.OVERLAP
                    }
                },
                onSelectedIdsChange = { selectedIds ->
                    viewModel.setOverlapSelectedAccountIds(selectedIds)
                },
                onConfirm = {
                    showOverlapAccountDialog = false
                },
                onDismiss = { showOverlapAccountDialog = false }
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

        // 学期选择弹窗
        if (showTermSelectionDialog) {
            TermSelectionDialog(
                termOptions = termOptions,
                currentXnm = xnm,
                currentXqm = xqm,
                onTermSelected = { xnm, xqm ->
                    viewModel.selectTerm(xnm, xqm)
                    showTermSelectionDialog = false
                },
                onDismiss = { showTermSelectionDialog = false }
            )
        }
    }
}
