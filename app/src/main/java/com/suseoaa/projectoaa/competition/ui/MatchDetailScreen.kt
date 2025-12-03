package com.suseoaa.projectoaa.competition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suseoaa.projectoaa.competition.model.MatchStatus
import com.suseoaa.projectoaa.competition.viewmodel.MatchDetailViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val detail = viewModel.matchDetail
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "加载中...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = "加载失败: $errorMessage",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                detail != null -> {
                    val status = detail.status
                    val isEnded = status == MatchStatus.ENDED

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        StatusBadge(status = status)
                        Spacer(Modifier.height(16.dp))

                        InfoRow(
                            icon = Icons.Default.MedicalInformation,
                            label = "比赛 ID",
                            value = detail.id.toString(),
                            isEnded = isEnded
                        )
                        Spacer(Modifier.height(6.dp))

                        InfoRow(
                            icon = Icons.Default.PersonOutline,
                            label = "发布者",
                            value = detail.organizerName,
                            isEnded = isEnded
                        )
                        Spacer(Modifier.height(6.dp))

                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = "报名时间",
                            value = detail.regTime.joinToString(" 到 "),
                            isEnded = isEnded
                        )
                        Spacer(Modifier.height(6.dp))

                        InfoRow(
                            icon = Icons.Default.Event,
                            label = "比赛时间",
                            value = detail.matchTime.joinToString(" 到 "),
                            isEnded = isEnded
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "比赛详情",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))

                        MarkdownText(
                            markdown = detail.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isEnded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// InfoRow 和 StatusBadge 保持不变 (引用自上一轮代码)
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isEnded: Boolean
) {
    val tint = if (isEnded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    val labelColor = if (isEnded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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