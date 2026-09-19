package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.checkin.ActivityCheckinViewModel
import com.suseoaa.projectoaa.presentation.checkin.ActivityCheckinUiState
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldCheckinTask
import com.suseoaa.projectoaa.util.PlatformPermissionManager

// 近场发现视图：扫描状态、任务卡片与空态。

/**
 * 扫描状态头部组件
 */
@Composable
fun ScanningStatusHeader(
    isScanning: Boolean,
    onToggleScan: () -> Unit
) {
    Surface(
        onClick = onToggleScan,
        shape = RoundedCornerShape(24.dp),
        color = if (isScanning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isScanning) Icons.Default.Radar else Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isScanning) "正在搜索附近任务..." else "未开启扫描",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isScanning) "请靠近活动发布者" else "点击开启近场发现",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 任务卡片组件
 */
@Composable
fun NearFieldTaskCard(
    task: NearFieldCheckinTask,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.activityName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "主持人: ${task.hostDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 无任务占位图
 */
@Composable
fun EmptyDiscoveryPlaceholder(isScanning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.WifiTetheringError,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isScanning) "附近暂无正在进行的签到" else "开启扫描以发现活动",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * 发现任务视图
 */
@Composable
internal fun DiscoveryView(
    uiState: ActivityCheckinUiState,
    viewModel: ActivityCheckinViewModel,
    permissionManager: PlatformPermissionManager,
    onCheckinClick: (NearFieldCheckinTask) -> Unit,
    onRequestPermissions: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        ScanningStatusHeader(
            isScanning = uiState.isScanning,
            onToggleScan = {
                if (permissionManager.hasNearFieldPermissions()) {
                    if (permissionManager.isHardwareEnabled()) {
                        viewModel.toggleScanning()
                    } else {
                        onRequestPermissions()
                    }
                } else {
                    onRequestPermissions()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "附近的签到任务 (${uiState.discoveredTasks.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.discoveredTasks.isEmpty()) {
            EmptyDiscoveryPlaceholder(uiState.isScanning)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.discoveredTasks) { task ->
                    NearFieldTaskCard(
                        task = task,
                        onClick = { onCheckinClick(task) }
                    )
                }
            }
        }
    }
}
