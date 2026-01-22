package com.suseoaa.projectoaa.feature.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.designsystem.theme.*
import com.suseoaa.projectoaa.core.network.model.announcement.FetchAnnouncementInfoResponse
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    isTablet: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val departments = viewModel.departments


    Scaffold(containerColor = OxygenBackground) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            with(sharedTransitionScope) {
                DepartmentGrid(
                    isTablet = isTablet,
                    departments = departments,
                    cardInfos = uiState.cardInfos,
                    onItemClick = onNavigateToDetail,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DepartmentGrid(
    isTablet: Boolean,
    departments: List<String>,
    cardInfos: Map<String, FetchAnnouncementInfoResponse.Data>,
    onItemClick: (String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val gridCells = if (isTablet) GridCells.Fixed(3) else GridCells.Fixed(2)
    // 头部跨列数
    val spanCount = if (isTablet) 3 else 2

    LazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 100.dp,
            start = 16.dp,
            end = 16.dp
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
                    color = InkGrey,
                    letterSpacing = 1.sp
                )
                Text(
                    "开放原子开源协会",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkBlack
                )
            }
        }

        // 1. 协会大卡片 (独占一行)
        item(span = { GridItemSpan(spanCount) }) {
            val dept = "协会"
            Box(
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "card_$dept"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = AcademicSharedTransitionSpec,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    zIndexInOverlay = 1f
                )
            ) {
                BigAssociationCard(
                    name = "开放原子开源协会",
                    data = cardInfos[dept],
                    onClick = { onItemClick(dept) }
                )
            }
        }

        // 2. 其他部门卡片
        val otherDepts = departments.filter { it != "协会" }
        items(otherDepts) { dept ->
            Box(
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "card_$dept"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = AcademicSharedTransitionSpec,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    zIndexInOverlay = 1f
                )
            ) {
                DepartmentCard(
                    name = dept,
                    data = cardInfos[dept],
                    icon = getIconForDepartment(dept),
                    onClick = { onItemClick(dept) }
                )
            }
        }
    }
}

// 样式 1：协会大卡片
@Composable
fun BigAssociationCard(
    name: String,
    data: FetchAnnouncementInfoResponse.Data?,
    onClick: () -> Unit
) {
    val summary = data?.data?.stripMarkdown() ?: "加载中..."

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        color = ElectricBlue,
        shadowElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 样式 2：普通部门卡片
@Composable
fun DepartmentCard(
    name: String,
    data: FetchAnnouncementInfoResponse.Data?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val summary = data?.data?.stripMarkdown() ?: "..."

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        color = OxygenWhite,
        border = BorderStroke(0.5.dp, OutlineSoft),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SoftBlueWait,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(20.dp), tint = ElectricBlue)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                    maxLines = 1
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (data == null) InkGrey.copy(alpha = 0.5f) else InkGrey,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = InkGrey.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// 简单的正则去除：标题符、粗体、链接格式、图片格式等
fun String.stripMarkdown(): String {
    return this
        .replace(Regex("^#{1,6}\\s*"), "") // 去除标题 #
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // 去除粗体 **text** -> text
        .replace(Regex("\\*(.*?)\\*"), "$1") // 去除斜体 *text* -> text
        .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // 去除图片 ![alt](url) -> ""
        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // 去除链接 [text](url) -> text
        .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1") // 去除代码块 `code` -> code
        .replace(Regex(">\\s*"), "") // 去除引用 >
        .replace("\n", " ") // 把换行变成空格，让预览更紧凑
        .trim()
}

fun getIconForDepartment(name: String): ImageVector {
    return when (name) {
        "协会" -> Icons.Default.Home
        "算法竞赛部" -> Icons.Default.Code
        "项目实践部" -> Icons.Default.Build
        "组织宣传部" -> Icons.Default.Campaign
//        "理事会" -> Icons.Default.Group
        "秘书处" -> Icons.Default.EditNote
        else -> Icons.Default.Star
    }
}