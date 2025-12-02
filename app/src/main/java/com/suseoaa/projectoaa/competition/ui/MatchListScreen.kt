package com.suseoaa.projectoaa.competition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
//滑动组件
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
//model
import com.suseoaa.projectoaa.competition.model.MatchItem
import com.suseoaa.projectoaa.competition.viewmodel.MatchListViewModel

/**
 * 比赛列表屏幕 (支持下拉刷新)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = hiltViewModel(),
    onNavigateToDetail: (Int) -> Unit
) {
    val matchList = viewModel.matchList
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar("错误: $errorMessage")
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("比赛列表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.fetchMatchList() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicatorAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (matchList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(matchList) { match ->
                            MatchListItem(
                                match = match,
                                onClick = {
                                    onNavigateToDetail(match.id)
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                else if (!isLoading && errorMessage == null) {
                    Text(
                        text = "暂无比赛",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (isLoading && matchList.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}


/**
 * 比赛列表中的单个卡片项
 */
@Composable
fun MatchListItem(match: MatchItem, onClick: () -> Unit) {
    // (修改) 1. 定义一个主题感知的颜色列表
    val itemColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    // (修改) 2. 根据 match.id 在前端确定性地选择颜色
    //    我们不再使用 match.color (来自后端)
    val itemColor = itemColors[match.id % itemColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 左侧颜色条
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(itemColor) // 3. 应用前端生成的颜色
            )

            // 右侧内容
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = match.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "报名时间",
                    value = match.regTime.joinToString(" 到 ")
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    icon = Icons.Default.Event,
                    label = "比赛时间",
                    value = match.matchTime.joinToString(" 到 ")
                )
            }
        }
    }
}

/**
 * 辅助 Composable, 用于显示 "图标 + 标签 + 值"
 */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}