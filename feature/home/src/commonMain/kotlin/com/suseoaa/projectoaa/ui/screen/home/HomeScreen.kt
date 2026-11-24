package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.presentation.home.HomeViewModel
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.DepartmentTypes
import com.suseoaa.projectoaa.ui.animation.sharedBoundsTransition
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.component.LocalMainTabVisible
import com.suseoaa.projectoaa.ui.component.WindowSizeClass
import com.suseoaa.projectoaa.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.suseoaa.projectoaa.util.AppPredictiveBackHandler
import com.suseoaa.projectoaa.util.PlatformPredictiveBackEvent
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToRecruitment: () -> Unit,
    onNavigateToUserQuery: () -> Unit,
    onNavigateToOrganization: () -> Unit = {},
    onNavigateToAppManagement: () -> Unit = {},
    onNavigateToValueCalculator: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
    featureDrawerExpanded: Boolean = false,
    onFeatureDrawerExpandedChange: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val isMainTabVisible = LocalMainTabVisible.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner, isMainTabVisible) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isMainTabVisible) {
                viewModel.refreshHomeSummary(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 手势进度状态：驱动抽屉实时跟随手势移动
    var backGestureProgress by remember { mutableStateOf<Float?>(null) }
    var backGestureCancelCount by remember { mutableIntStateOf(0) }

    AppPredictiveBackHandler(
        enabled = featureDrawerExpanded,
        onProgress = { event -> backGestureProgress = event.progress },
        onCancel = {
            backGestureProgress = null
            backGestureCancelCount++
        },
        onBack = {
            backGestureProgress = null
            onFeatureDrawerExpandedChange(false)
        }
    )

    HomeWithDrawer(
        currentUser = uiState.currentUser,
        isExpanded = featureDrawerExpanded,
        onExpandedChange = onFeatureDrawerExpandedChange,
        // 招新换届需要协会账号，游客点进去先登录
        onNavigateToRecruitment = if (uiState.isSignedIn == false) onNavigateToLogin else onNavigateToRecruitment,
        onNavigateToUserQuery = onNavigateToUserQuery,
        onNavigateToOrganization = onNavigateToOrganization,
        onNavigateToAppManagement = onNavigateToAppManagement,
        onNavigateToValueCalculator = onNavigateToValueCalculator,
        bottomBarHeight = bottomBarHeight,
        backGestureProgress = backGestureProgress,
        backGestureCancelCount = backGestureCancelCount
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshHomeSummary(showLoading = true) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row {
                    //            部门介绍卡片
                    DepartmentGrid(
                        departments = uiState.departments,
                        announcements = uiState.activeAnnouncements,
                        hasLoadedAnnouncements = uiState.hasLoadedAnnouncements,
                        onItemClick = onNavigateToDetail,
                        bottomBarHeight = bottomBarHeight,
                        guestPrompt = if (uiState.isSignedIn != false) null else {
                            { GuestLoginCard(onLogin = onNavigateToLogin) }
                        }
                    )
                    //
                }
            }
        }
    }
}

@Composable
fun DepartmentGrid(
    departments: List<Department>,
    announcements: Map<String, Announcement>,
    hasLoadedAnnouncements: Boolean,
    onItemClick: (String) -> Unit,
    bottomBarHeight: Dp = 0.dp,
    /** 游客模式下代替部门卡片显示的登录引导 */
    guestPrompt: (@Composable () -> Unit)? = null
) {
    val summaryByDept = remember(announcements) {
        announcements.mapValues { (_, announcement) -> announcement.summary() }
    }
    fun summaryOf(name: String): String? = summaryByDept[name]
        ?: if (hasLoadedAnnouncements) "暂无公告" else null
    val association = departments.firstOrNull { it.type == DepartmentTypes.ASSOCIATION }
    val otherDepts = departments.filter { it != association }

    AdaptiveLayout { config ->
        val spanCount = config.gridColumns
        val gridCells = GridCells.Fixed(spanCount)
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val horizontalPadding = config.horizontalPadding

        LazyVerticalGrid(
            columns = gridCells,
            contentPadding = PaddingValues(
                top = 16.dp + statusBarHeight,
                bottom = 16.dp + bottomBarHeight,
                start = horizontalPadding,
                end = horizontalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 头部标题
            item(span = { GridItemSpan(spanCount) }) {
                Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                    Text(
                        "四川轻化工大学",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "开放原子开源协会",
                        style = MaterialTheme
                            .typography
                            .headlineLarge
                            .copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (guestPrompt != null) {
                item(span = { GridItemSpan(spanCount) }) { guestPrompt() }
            }

            // 1. 协会大卡片 (独占一行)
            if (association != null) {
                item(span = { GridItemSpan(spanCount) }) {
                    BigAssociationCard(
                        name = association.name,
                        summary = summaryOf(association.name),
                        onClick = { onItemClick(association.name) }
                    )
                }
            }

            // 2. 其他部门卡片
            items(otherDepts, key = { it.id }) { dept ->
                DepartmentCard(
                    name = dept.name,
                    summary = summaryOf(dept.name),
                    icon = getIconForDepartment(dept.name),
                    onClick = { onItemClick(dept.name) }
                )
            }
        }
    }
}

/** 游客模式的首页引导：部门与公告需要协会账号，其余功能在上拉抽屉里可直接用。 */
@Composable
private fun GuestLoginCard(onLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "登录后查看各部门公告",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "部门公告、招新换届需要协会账号。课表、教务查询和上拉抽屉中的物品价值计算无需登录即可使用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onLogin) {
                Text("登录 / 注册")
            }
        }
    }
}

// 样式 1：协会大卡片
@Composable
fun BigAssociationCard(
    name: String,
    summary: String?,
    onClick: () -> Unit
) {
    val summaryText = summary ?: "加载中..."

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .sharedBoundsTransition("department_$name"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 样式 2：普通部门卡片
@Composable
fun DepartmentCard(
    name: String,
    summary: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val summaryText = summary ?: "..."

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .sharedBoundsTransition("department_$name"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = if (summary == null) MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
                    .copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** 首页卡片摘要：标题 + 去掉 Markdown 的正文。 */
private fun Announcement.summary(): String {
    val body = content.stripMarkdown()
    return if (title.isBlank()) body else "$title：$body"
}

/**
 * 去除 Markdown 格式符号，返回纯文本
 * 采用逐行处理 + 字符替换的方式，避免正则匹配不完整的问题
 */
fun String.stripMarkdown(): String {
    return this
        .lines().joinToString(" ") { line ->
            var result = line
            // 去除标题符号 (行首的 # 符号)
            while (result.startsWith("#")) {
                result = result.removePrefix("#")
            }
            result = result.trimStart()  // 去除标题后的空格
            result
        }  // 用空格连接各行
        .let { text ->
            var result = text
            // 去除粗体 **text** -> text
            while (result.contains("**")) {
                val start = result.indexOf("**")
                val end = result.indexOf("**", start + 2)
                if (end > start) {
                    result = result.substring(0, start) +
                            result.substring(start + 2, end) +
                            result.substring(end + 2)
                } else {
                    // 没有配对的 **，直接移除
                    result = result.replaceFirst("**", "")
                }
            }
            // 去除斜体 *text* -> text (单个星号)
            while (result.contains("*")) {
                val start = result.indexOf("*")
                val end = result.indexOf("*", start + 1)
                if (end > start) {
                    result = result.substring(0, start) +
                            result.substring(start + 1, end) +
                            result.substring(end + 1)
                } else {
                    result = result.replaceFirst("*", "")
                }
            }
            // 去除行内代码 `code` -> code
            while (result.contains("`")) {
                val start = result.indexOf("`")
                val end = result.indexOf("`", start + 1)
                if (end > start) {
                    result = result.substring(0, start) +
                            result.substring(start + 1, end) +
                            result.substring(end + 1)
                } else {
                    result = result.replaceFirst("`", "")
                }
            }
            result
        }
        // 去除图片 ![alt](url)
        .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
        // 去除链接 [text](url) -> text
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        // 去除引用符号 >
        .replace(Regex("^>+\\s*", RegexOption.MULTILINE), "")
        // 去除多余空格
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun getIconForDepartment(name: String): ImageVector {
    return when (name) {
        "协会" -> Icons.Default.Home
        "算法竞赛部" -> Icons.Default.Star
        "项目实践部" -> Icons.Default.Build
        "组织宣传部" -> Icons.Default.Notifications
        "秘书处" -> Icons.Default.Create
        else -> Icons.Default.Star
    }
}
