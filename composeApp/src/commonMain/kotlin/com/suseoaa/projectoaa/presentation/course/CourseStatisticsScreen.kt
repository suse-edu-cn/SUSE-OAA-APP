package com.suseoaa.projectoaa.presentation.course

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.util.LockScreenOrientation
import com.suseoaa.projectoaa.ui.component.isTabletFormFactorDevice
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseStatisticsScreen(
    onBack: () -> Unit,
    viewModel: CourseStatisticsViewModel = koinViewModel()
) {
    // 强制横屏
    LockScreenOrientation(landscape = true)

    val timelineData by viewModel.timelineData.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val selectedTerms by viewModel.selectedTerms.collectAsStateWithLifecycle()
    val availableTerms by viewModel.availableTerms.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showSidePanel by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 全端统一：左侧侧滑挤出，右侧时间线
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = showSidePanel) {
                Box(modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                ) {
                    FilterPanel(false, viewModel, allAccounts, selectedAccountIds, selectedTerms, availableTerms)
                    VerticalDivider(modifier = Modifier.align(Alignment.CenterEnd), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TimelineView(timelineData)
                TopControlButtons(
                    onBack = onBack,
                    isSyncing = isSyncing,
                    onSync = { viewModel.syncAllData() },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) }
                )
                
                // 侧滑控制按钮，悬浮在左侧偏上一点
                IconButton(
                    onClick = { showSidePanel = !showSidePanel },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (showSidePanel) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "切换筛选器",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TopControlButtons(
    onBack: () -> Unit,
    isSyncing: Boolean,
    onSync: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // 悬浮返回按钮 (固定在左上角)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 顶部搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("搜索课程/教师") },
            singleLine = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
                .widthIn(max = 300.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        )

        // 同步按钮 (固定在右上角)
        IconButton(
            onClick = onSync,
            enabled = !isSyncing,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "同步",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TimelineView(
    timelineData: Map<String, Map<Pair<String, String>, Map<String, List<CourseNodeData>>>>
) {
    if (timelineData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据，请尝试调整筛选或搜索条件", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    } else {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape) // 防止无边界画布溢出到控制栏或其他区域
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(0.05f, 3f)
                        
                        // 基于 TransformOrigin(0f, 0f) 的中心点缩放+平移算法
                        val canvasPoint = (centroid - offset) / oldScale
                        offset = centroid + pan - canvasPoint * scale
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            // 双击恢复默认缩放和位置
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true) // 核心：让内部视图不再受限于屏幕宽高，全量预渲染！
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .padding(top = 96.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                    timelineData.forEach { (studentId, studentTerms) ->
                        val firstCourse = studentTerms.values.firstOrNull()?.values?.firstOrNull()?.firstOrNull()
                        val studentName = firstCourse?.studentName ?: studentId
                        
                        Column {
                            Surface(
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "👤 ${studentName} 的学习轨迹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.wrapContentWidth().padding(horizontal = 64.dp),
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                studentTerms.forEach { (termPair, teacherMap) ->
                                    val (xnm, xqm) = termPair
                                    val termName = "${xnm}-${xnm.toInt() + 1}学年 第${if (xqm == "3") "一" else "二"}学期"
                                    
                                    TermTimelineNode(termName = termName, teacherMap = teacherMap)
                                }
                            }
                        }
                    }
            }
        }
    }
}


@Composable
private fun FilterPanel(
    isTablet: Boolean,
    viewModel: CourseStatisticsViewModel,
    allAccounts: List<com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity>,
    selectedAccountIds: Set<String>,
    selectedTerms: Set<Pair<String, String>>,
    availableTerms: List<Pair<String, String>>
) {
    if (isTablet) {
        // 平板端左右分栏布局
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // 左半边：账号列表
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = "账号配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allAccounts) { account ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAccount(account.studentId) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedAccountIds.contains(account.studentId),
                                onCheckedChange = { viewModel.toggleAccount(account.studentId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${account.name} (${account.studentId})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            // 右半边：学期列表
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = "学期配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectAllTerms() }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedTerms.isEmpty(),
                                onCheckedChange = { viewModel.selectAllTerms() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "全部学期",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTerms.isEmpty()) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    items(availableTerms) { termPair ->
                        val (xnm, xqm) = termPair
                        val isSelected = selectedTerms.contains(termPair) || selectedTerms.isEmpty()
                        val termName = "${xnm}-${xnm.toInt() + 1}学年 第${if (xqm == "3") "一" else "二"}学期"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTerm(xnm, xqm) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleTerm(xnm, xqm) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = termName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 手机端单列布局
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "数据配置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // ===== 账号筛选 =====
            item {
                Text(
                    text = "参与统计的账号 (可多选对比)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(allAccounts) { account ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAccount(account.studentId) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedAccountIds.contains(account.studentId),
                        onCheckedChange = { viewModel.toggleAccount(account.studentId) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${account.name} (${account.studentId})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // ===== 学期筛选 =====
            item {
                Text(
                    text = "学期范围 (可多选)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAllTerms() }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedTerms.isEmpty(),
                        onCheckedChange = { viewModel.selectAllTerms() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "全部学期",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selectedTerms.isEmpty()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            items(availableTerms) { termPair ->
                val (xnm, xqm) = termPair
                val isSelected = selectedTerms.contains(termPair) || selectedTerms.isEmpty()
                val termName = "${xnm}-${xnm.toInt() + 1}学年 第${if (xqm == "3") "一" else "二"}学期"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleTerm(xnm, xqm) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleTerm(xnm, xqm) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = termName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TermTimelineNode(
    termName: String,
    teacherMap: Map<String, List<CourseNodeData>>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Column(
        modifier = Modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间轴核心绘制区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 200.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 绘制贯穿左右的时间主线
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 6f
                )
                // 绘制当前学期的节点圆圈
                drawCircle(
                    color = primaryColor,
                    radius = 16f,
                    center = Offset(size.width / 2, size.height / 2)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            // 学期文字
            Text(
                text = termName,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.offset(y = (-28).dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 教师与课程横向延展
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            teacherMap.forEach { (teacher, courses) ->
                TeacherBranch(teacherName = teacher, courses = courses)
            }
        }
    }
}

@Composable
fun TeacherBranch(teacherName: String, courses: List<CourseNodeData>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        // 教师卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(min = 80.dp)
        ) {
            Text(
                text = teacherName,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 向下连线
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 课程列表
        courses.forEach { node ->
            CourseChip(node)
        }
    }
}

@Composable
fun CourseChip(node: CourseNodeData) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = node.courseName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            node.primaryGrade?.let { grade ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (grade.regularScore.isNotEmpty() || grade.finalScore.isNotEmpty()) {
                            Text(
                                text = "平:${grade.regularScore.ifEmpty { "-" }} | 期:${grade.finalScore.ifEmpty { "-" }}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (grade.experimentScore.isNotEmpty()) {
                            Text(
                                text = "实验:${grade.experimentScore}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = grade.score,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            node.makeupGrade?.let { makeup ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "补考",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = makeup.score,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
