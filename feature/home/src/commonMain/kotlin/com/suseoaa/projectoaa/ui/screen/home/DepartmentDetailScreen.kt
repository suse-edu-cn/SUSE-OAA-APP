package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.suseoaa.projectoaa.presentation.home.AnnouncementViewModel
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.ui.animation.pageShellBounds
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.OaaMarkdownText
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/**
 * 部门详情：当前公告、草稿（有权限时）与历史公告。
 *
 * @param onNavigateToEdit 传入公告 ID 编辑已有公告，传 null 新建
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    departmentName: String,
    onBack: () -> Unit,
    onNavigateToEdit: (announcementId: Int?) -> Unit,
    viewModel: AnnouncementViewModel = koinViewModel()
) {
    val rawUiState by viewModel.uiState.collectAsState()
    val uiState = if (rawUiState.departmentName == departmentName) {
        rawUiState
    } else {
        remember(departmentName) { viewModel.snapshotFromCache(departmentName) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingDelete by remember { mutableStateOf<Announcement?>(null) }

    // 从编辑页返回时也要刷新，所以挂在 ON_RESUME 上
    DisposableEffect(lifecycleOwner, departmentName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load(departmentName, forceRefresh = false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            ToastManager.showToast(it)
            viewModel.clearMessage()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pageShellBounds("department_$departmentName"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .height(64.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                    Text(
                        departmentName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (uiState.isSubmitting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val hasCachedContent = uiState.hasLoadedCache ||
                    uiState.active.isNotEmpty() ||
                    uiState.history.isNotEmpty() ||
                    uiState.drafts.isNotEmpty()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.load(departmentName, forceRefresh = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    when {
                        uiState.isLoading && !hasCachedContent -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        uiState.error != null && uiState.active.isEmpty() -> {
                            ErrorContent(
                                error = uiState.error ?: "未知错误",
                                onRetry = { viewModel.load(departmentName, forceRefresh = true) },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> AnnouncementList(
                            active = uiState.active,
                            drafts = uiState.drafts,
                            history = uiState.history,
                            canManage = uiState.canManage,
                            onEdit = { onNavigateToEdit(it.id) },
                            onPublish = viewModel::publish,
                            onDelete = { pendingDelete = it }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.canManage,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                FloatingActionButton(
                    modifier = Modifier.sharedBoundsTransition("department_edit_$departmentName"),
                    onClick = { onNavigateToEdit(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "新建公告")
                }
            }
        }
    }

    pendingDelete?.let { announcement ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除公告") },
            text = { Text("确定删除「${announcement.title}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(announcement)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AnnouncementList(
    active: List<Announcement>,
    drafts: List<Announcement>,
    history: List<Announcement>,
    canManage: Boolean,
    onEdit: (Announcement) -> Unit,
    onPublish: (Announcement) -> Unit,
    onDelete: (Announcement) -> Unit
) {
    var historyExpanded by remember { mutableStateOf(false) }
    var expandedHistoryId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (active.isEmpty()) {
            item {
                Text(
                    "该部门暂无公告",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        items(active, key = { "active_${it.id}" }) { announcement ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    announcement.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    announcement.metaLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OaaMarkdownText(
                    markdown = announcement.content,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                )
                if (canManage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(announcement) }) { Text("编辑") }
                    }
                }
            }
        }

        if (canManage && drafts.isNotEmpty()) {
            item { SectionTitle("草稿（${drafts.size}）") }
            items(drafts, key = { "draft_${it.id}" }) { draft ->
                AnnouncementCard(
                    announcement = draft,
                    subtitle = "最后修改 ${formatDateTime(draft.updatedAt)}",
                    actions = {
                        TextButton(onClick = { onEdit(draft) }) { Text("编辑") }
                        TextButton(onClick = { onPublish(draft) }) { Text("发布") }
                        TextButton(onClick = { onDelete(draft) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }

        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { historyExpanded = !historyExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("历史公告（${history.size}）", modifier = Modifier.weight(1f))
                    Icon(
                        if (historyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (historyExpanded) "收起" else "展开"
                    )
                }
            }
            if (historyExpanded) {
                items(history, key = { "history_${it.id}" }) { item ->
                    val expanded = expandedHistoryId == item.id
                    AnnouncementCard(
                        announcement = item,
                        subtitle = item.metaLine(),
                        onClick = { expandedHistoryId = if (expanded) null else item.id },
                        body = if (expanded) item.content else null,
                        actions = {
                            if (canManage) {
                                TextButton(onClick = { onEdit(item) }) { Text("编辑") }
                                TextButton(onClick = { onPublish(item) }) { Text("设为当前") }
                                TextButton(onClick = { onDelete(item) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 12.dp)
    )
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    body: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(announcement.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (body != null) {
                OaaMarkdownText(
                    markdown = body,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                content = actions
            )
        }
    }
}

private fun Announcement.metaLine(): String = buildString {
    if (publisherName.isNotBlank()) {
        append(publisherName)
        if (publisherRole.isNotBlank()) append(" · ").append(publisherRole)
        append("　")
    }
    append("发布于 ").append(formatDateTime(publishedAt.orEmpty()))
}

/** 2026-09-01T00:00:00+08:00 -> 2026-09-01 00:00 */
internal fun formatDateTime(raw: String): String =
    raw.take(16).replace('T', ' ').ifBlank { "未知时间" }

/**
 * 错误内容
 */
@Composable
internal fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            "加载失败",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
