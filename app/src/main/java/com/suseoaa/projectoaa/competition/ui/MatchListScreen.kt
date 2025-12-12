package com.suseoaa.projectoaa.competition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.suseoaa.projectoaa.competition.model.MatchListUiItem
import com.suseoaa.projectoaa.competition.model.MatchStatus
import com.suseoaa.projectoaa.competition.viewmodel.MatchListViewModel

/**
 * 比赛列表屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = hiltViewModel(),
    onNavigateToDetail:(Int) -> Unit,
    onNavigateBack: () -> Unit //接收返回回调
    //TODO:导航界面  onNavigateToCreate:() -> Unit
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
                //导航图标配置
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
        //TODO:暂时隐藏创建入口，待权限系统完善后启动
        /*
        floatingActionButton = {
            FloatingActionButton(onClick = {onNavigateToCreate()}){
                Icon(Icons.Default.Add, contentDescription = "创建比赛")
            }
         }
         */
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
                //无论是否有数据，都使用 LazyColumn，保证任何时候都能检测下滑手势
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    // 当内容不足一屏时，保证 LazyColumn 依然可以滚动，从而触发刷新（视 Compose 版本而定，通常 fillMaxSize 即可）
                ) {
                    if (matchList.isNotEmpty()) {
                        items(matchList) { match ->
                            MatchListItem(
                                match = match,
                                onClick = {
                                    onNavigateToDetail(match.id)
                                }
                            )
                            val spacing = if (match.status == MatchStatus.ENDED) 8.dp else 14.dp
                            Spacer(Modifier.height(spacing))
                        }
                    } else if (!isLoading && errorMessage == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "现在还没有比赛",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
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
fun MatchListItem(
    match: MatchListUiItem,
    onClick: () -> Unit
) {
    val isEnded = match.status == MatchStatus.ENDED

    val itemColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    val itemColor = itemColors[match.id % itemColors.size]

    // 根据状态决定侧边栏和标题颜色
    val sideBarColor = if (isEnded) MaterialTheme.colorScheme.outlineVariant else itemColor
    val titleColor = if (isEnded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val cardElevation = if (isEnded) 2.dp else 6.dp
    val cardModifier = if (isEnded) {
        Modifier.fillMaxWidth() //宽一点
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp) //窄一点
    }

    Card(
        modifier = cardModifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(sideBarColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusBadge(status = match.status)
                    DynamicTimeInfoRow(match = match, isEnded = isEnded)
                }
            }
        }
    }
}

/**
 * 动态时间显示组件 (根据比赛状态显示报名或比赛时间)
 */
@Composable
private fun DynamicTimeInfoRow(match: MatchListUiItem, isEnded: Boolean) {
    val icon: ImageVector
    val label: String
    val time: List<String>

    when (match.status) {
        MatchStatus.REGISTERING, MatchStatus.UPCOMING -> {
            icon = Icons.Default.CalendarToday
            label = "报名"
            time = match.regTime
        }
        MatchStatus.REGISTRATION_ENDED, MatchStatus.ONGOING, MatchStatus.ENDED -> {
            icon = Icons.Default.Event
            label = "比赛"
            time = match.matchTime
        }
    }

    // 根据 'isEnded' 状态决定图标和标签的颜色
    val iconTint = if (isEnded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    val labelColor = if (isEnded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary

    InfoRow(
        icon = icon,
        label = label,
        value = time.joinToString(" - "),
        iconTint = iconTint,
        labelColor = labelColor
    )
}


/**
 * 状态徽章 Composable
 */
@Composable
private fun StatusBadge(status: MatchStatus) {
    val (text, color) = when (status) {
        MatchStatus.REGISTERING -> "报名中" to MaterialTheme.colorScheme.error
        MatchStatus.ONGOING -> "比赛中" to MaterialTheme.colorScheme.primary
        MatchStatus.UPCOMING -> "筹备中" to MaterialTheme.colorScheme.secondary
        MatchStatus.REGISTRATION_ENDED -> "即将比赛" to MaterialTheme.colorScheme.tertiary
        MatchStatus.ENDED -> "已结束" to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * 辅助 Composable, 格式: [Icon] [Label] [Value]
 */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    labelColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconTint
        )
        Spacer(Modifier.width(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}