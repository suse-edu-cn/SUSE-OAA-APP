package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.checkin.CheckinViewModel
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.getDetailColumns
import org.koin.compose.viewmodel.koinViewModel

// 单个账号下的签到任务列表页。

@Composable
fun CheckinTaskScreen(
    accountId: Long,
    onBack: () -> Unit,
    viewModel: CheckinViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val account = uiState.accounts.firstOrNull { it.id == accountId }

    LaunchedEffect(accountId, account?.id, uiState.selectedAccount?.id) {
        if (accountId > 0 && account != null && uiState.selectedAccount?.id != accountId) {
            viewModel.loadTasksForAccount(account)
        }
    }

    DisposableEffect(accountId) {
        onDispose {
            viewModel.clearTasks()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.accounts.isEmpty() && uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            account == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "账号不存在或已被删除",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onBack) {
                        Text("返回")
                    }
                }
            }

            else -> {
                TaskListView(
                    viewModel = viewModel,
                    uiState = uiState,
                    account = account,
                    onBack = onBack
                )
            }
        }
    }
}

/**
 * 任务列表视图
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskListView(
    viewModel: CheckinViewModel,
    uiState: com.suseoaa.projectoaa.presentation.checkin.CheckinUiState,
    account: CheckinAccountData,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .sharedBoundsTransition("checkin_account_${account.id}")
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部栏
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name.ifEmpty { account.studentId },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "待打卡: ${uiState.pendingTasks.size} | 已打卡: ${uiState.completedTasks.size} | 缺勤: ${uiState.absentTasks.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.loadTasksForAccount(account) }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        }

        // 筛选Tab
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("全部\n(${uiState.pendingTasks.size + uiState.completedTasks.size + uiState.absentTasks.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("待打卡\n(${uiState.pendingTasks.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("已打卡\n(${uiState.completedTasks.size})") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("缺勤\n(${uiState.absentTasks.size})") }
            )
        }

        if (uiState.isLoadingTasks) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AdaptiveLayout(modifier = Modifier.fillMaxSize()) { adaptiveLayoutConfig ->
                val columns = adaptiveLayoutConfig.getDetailColumns()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 根据选中Tab显示不同的任务
                    when (selectedTab) {
                        0 -> {
                            // 全部任务
                            if (uiState.pendingTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "待打卡任务 (${uiState.pendingTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskPendingColor()
                                    )
                                }
                                items(
                                    count = (uiState.pendingTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.pendingTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.pendingTasks[index],
                                                        status = 1,
                                                        isChecking = uiState.checkingTaskId == uiState.pendingTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.pendingTasks[index],
                                                                allowRepeat = false
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.completedTasks.isNotEmpty()) {
                                // 只显示 displayedCompletedCount 个任务
                                val displayCount = minOf(
                                    uiState.displayedCompletedCount,
                                    uiState.completedTasks.size
                                )
                                val hasMore =
                                    uiState.completedTasks.size > uiState.displayedCompletedCount

                                item {
                                    Text(
                                        text = "已打卡任务 (${uiState.completedTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskCompletedColor()
                                    )
                                }
                                items(
                                    count = (displayCount + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < displayCount) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.completedTasks[index],
                                                        status = 2,
                                                        isChecking = uiState.checkingTaskId == uiState.completedTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.completedTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // 加载更多按钮
                                if (hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.loadMoreCompletedTasks() },
                                                enabled = !uiState.isLoadingMoreCompleted,
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                if (uiState.isLoadingMoreCompleted) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("加载中...")
                                                } else {
                                                    Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.absentTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "缺勤任务 (${uiState.absentTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getTaskAbsentColor()
                                    )
                                }
                                items(
                                    count = (uiState.absentTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.absentTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.absentTasks[index],
                                                        status = 3,
                                                        isChecking = uiState.checkingTaskId == uiState.absentTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.absentTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.pendingTasks.isEmpty() && uiState.completedTasks.isEmpty() && uiState.absentTasks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // 仅待打卡任务
                            if (uiState.pendingTasks.isNotEmpty()) {
                                items(
                                    count = (uiState.pendingTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.pendingTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.pendingTasks[index],
                                                        status = 1,
                                                        isChecking = uiState.checkingTaskId == uiState.pendingTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.pendingTasks[index],
                                                                allowRepeat = false
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无待打卡任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // 仅已打卡任务
                            if (uiState.completedTasks.isNotEmpty()) {
                                // 只显示 displayedCompletedCount 个任务
                                val displayCount = minOf(
                                    uiState.displayedCompletedCount,
                                    uiState.completedTasks.size
                                )
                                val hasMore =
                                    uiState.completedTasks.size > uiState.displayedCompletedCount

                                items(
                                    count = (displayCount + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < displayCount) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.completedTasks[index],
                                                        status = 2,
                                                        isChecking = uiState.checkingTaskId == uiState.completedTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.completedTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // 加载更多按钮
                                if (hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.loadMoreCompletedTasks() },
                                                enabled = !uiState.isLoadingMoreCompleted,
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                if (uiState.isLoadingMoreCompleted) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("加载中...")
                                                } else {
                                                    Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无已打卡任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // 仅缺勤任务
                            if (uiState.absentTasks.isNotEmpty()) {
                                items(
                                    count = (uiState.absentTasks.size + columns - 1) / columns
                                ) { rowIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (columnIndex in 0 until columns) {
                                            val index = rowIndex * columns + columnIndex
                                            if (index < uiState.absentTasks.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TaskCard(
                                                        task = uiState.absentTasks[index],
                                                        status = 3,
                                                        isChecking = uiState.checkingTaskId == uiState.absentTasks[index].id,
                                                        onCheckin = {
                                                            viewModel.checkinForTask(
                                                                uiState.absentTasks[index],
                                                                allowRepeat = true
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "暂无缺勤任务",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    if (uiState.pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "待打卡任务 (${uiState.pendingTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskPendingColor()
                            )
                        }
                        items(
                            count = (uiState.pendingTasks.size + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < uiState.pendingTasks.size) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.pendingTasks[index],
                                                status = 1,
                                                isChecking = uiState.checkingTaskId == uiState.pendingTasks[index].id,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.pendingTasks[index],
                                                        allowRepeat = false
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 缺勤任务（未打卡）
                    if (uiState.absentTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "缺勤任务 (${uiState.absentTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskAbsentColor()
                            )
                        }
                        items(
                            count = (uiState.absentTasks.size + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < uiState.absentTasks.size) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.absentTasks[index],
                                                status = 3,
                                                isChecking = uiState.checkingTaskId == uiState.absentTasks[index].id,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.absentTasks[index],
                                                        allowRepeat = true
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 已打卡任务
                    if (uiState.completedTasks.isNotEmpty()) {
                        // 只显示 displayedCompletedCount 个任务
                        val displayCount =
                            minOf(uiState.displayedCompletedCount, uiState.completedTasks.size)
                        val hasMore = uiState.completedTasks.size > uiState.displayedCompletedCount

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已打卡任务 (${uiState.completedTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = getTaskCompletedColor()
                            )
                        }
                        items(
                            count = (displayCount + columns - 1) / columns
                        ) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (columnIndex in 0 until columns) {
                                    val index = rowIndex * columns + columnIndex
                                    if (index < displayCount) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TaskCard(
                                                task = uiState.completedTasks[index],
                                                status = 2,
                                                isChecking = uiState.checkingTaskId == uiState.completedTasks[index].id,
                                                onCheckin = {
                                                    viewModel.checkinForTask(
                                                        uiState.completedTasks[index],
                                                        allowRepeat = true
                                                    )
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // 加载更多按钮
                        if (hasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.loadMoreCompletedTasks() },
                                        enabled = !uiState.isLoadingMoreCompleted,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        if (uiState.isLoadingMoreCompleted) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("加载中...")
                                        } else {
                                            Text("加载更多 (${uiState.completedTasks.size - displayCount})")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 空状态
                    if (uiState.pendingTasks.isEmpty() && uiState.completedTasks.isEmpty() && uiState.absentTasks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无任务",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/**
 * 任务卡片
 */
@Composable
private fun TaskCard(
    task: com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask,
    status: Int,  // 1=待打卡, 2=已打卡, 3=缺勤
    isChecking: Boolean,
    onCheckin: () -> Unit
) {
    val statusText = when (status) {
        1 -> "待打卡"
        2 -> "已打卡"
        3 -> "缺勤"
        else -> "未知"
    }

    val statusColor = when (status) {
        1 -> getTaskPendingColor()
        2 -> getTaskCompletedColor()
        3 -> getTaskAbsentColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardColor = when (status) {
        1 -> getTaskPendingBgColor()
        2 -> getTaskCompletedBgColor()
        3 -> getTaskAbsentBgColor()
        else -> MaterialTheme.colorScheme.surface
    }

    val statusTextColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.White
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == 2) 0.dp else 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 任务名称和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.rwmc,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 显示完整日期和时间
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = task.needTime.ifEmpty { task.qdksrq },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${task.qdkssj} - ${task.qdjssj}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 显示已打卡时间（如果有）
                    if (status == 2 && !task.qdsj.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = getTaskCompletedColor()
                            )
                            Text(
                                text = "打卡于 ${task.qdsj?.substringAfter(" ")?.take(5) ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = getTaskCompletedColor()
                            )
                        }
                    }
                }

                // 状态标签
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (status == 2 && !task.qdsj.isNullOrBlank()) {
                            "已打卡"
                        } else {
                            statusText
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // 打卡按钮
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCheckin,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (status == 2 || status == 3) "再次打卡中..." else "打卡中...")
                } else {
                    Icon(
                        if (status == 2 || status == 3) Icons.Default.Refresh else Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (status == 2 || status == 3) "再次打卡" else "立即打卡")
                }
            }
        }
    }
}
