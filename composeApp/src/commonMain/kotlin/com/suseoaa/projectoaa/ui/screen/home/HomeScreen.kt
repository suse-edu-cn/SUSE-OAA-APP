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
import com.suseoaa.projectoaa.data.model.AnnouncementData
import com.suseoaa.projectoaa.ui.component.AdaptiveLayout
import com.suseoaa.projectoaa.ui.component.AdaptiveLayoutConfig
import com.suseoaa.projectoaa.ui.component.WindowSizeClass
import com.suseoaa.projectoaa.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToRecruitment: () -> Unit,
    bottomBarHeight: Dp = 0.dp,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val departments = viewModel.departments

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DepartmentGrid(
            departments = departments,
            cardInfos = uiState.cardInfos,
            onItemClick = onNavigateToDetail,
            onRecruitmentClick = onNavigateToRecruitment,
            bottomBarHeight = bottomBarHeight
        )
    }
}

@Composable
fun DepartmentGrid(
    departments: List<String>,
    cardInfos: Map<String, AnnouncementData?>,
    onItemClick: (String) -> Unit,
    onRecruitmentClick: () -> Unit,
    bottomBarHeight: Dp = 0.dp
) {
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

            // 1. 协会大卡片 (独占一行)
            item(span = { GridItemSpan(spanCount) }) {
                BigAssociationCard(
                    name = "开放原子开源协会",
                    data = cardInfos["协会"],
                    onClick = { onItemClick("协会") }
                )
            }

            // 2. 其他部门卡片
            val otherDepts = departments.filter { it != "协会" }
            items(otherDepts) { dept ->
                DepartmentCard(
                    name = dept,
                    data = cardInfos[dept],
                    icon = getIconForDepartment(dept),
                    onClick = { onItemClick(dept) }
                )
            }

            item(span = { GridItemSpan(1) }) {
                DepartmentCard(
                    name = "招新换届",
//                    TODO 这里记得改数据源
                    data = null,
                    icon = Icons.Default.Person,
                    onClick = onRecruitmentClick
                )
            }
        }
    }
}

// 样式 1：协会大卡片
@Composable
fun BigAssociationCard(
    name: String,
    data: AnnouncementData?,
    onClick: () -> Unit
) {
    val summary = data?.data?.stripMarkdown() ?: "加载中..."

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
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
                    text = summary,
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
    data: AnnouncementData?,
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
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (data == null) MaterialTheme
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
