package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.database.NearFieldTask
import com.suseoaa.projectoaa.shared.database.NearFieldParticipant

// 历史记录视图与参与者名单面板。

/**
 * 历史记录视图
 */
@Composable
internal fun HistoryView(
    history: List<NearFieldTask>,
    onTaskClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRestartClick: (NearFieldTask) -> Unit
) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无签到历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskClick(task.taskIdentifier) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    task.activityName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (task.isMyHosted == 1L) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "发起的",
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "时间: ${formatTimestamp(task.startTime)} - ${formatTimestamp(task.endTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            if (task.isMyHosted == 1L) {
                                IconButton(onClick = { onRestartClick(task) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "重新开启",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteClick(task.taskIdentifier) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 参与者详情底栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParticipantsBottomSheet(
    participants: List<NearFieldParticipant>,
    isMyHosted: Boolean,
    onDismiss: () -> Unit,
    onManualAdd: () -> Unit,
    onSync: () -> Unit,
    onDeleteParticipant: (Long) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "签到名单 (${participants.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onSync) {
                        Icon(Icons.Default.Refresh, contentDescription = "同步")
                    }
                    if (isMyHosted) {
                        TextButton(onClick = onManualAdd) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("补签")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无人员签到", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    items(participants) { p ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.participantName, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = when (p.status) {
                                            "正常" -> Color(0xFFE8F5E9)
                                            "迟到" -> Color(0xFFFFF3E0)
                                            "补签" -> Color(0xFFE3F2FD)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            p.status,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (p.status) {
                                                "正常" -> Color(0xFF2E7D32)
                                                "迟到" -> Color(0xFFEF6C00)
                                                "补签" -> Color(0xFF1565C0)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            },
                            supportingContent = { Text("学号: ${p.participantId}") },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatTimestamp(p.checkinTime),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isMyHosted) {
                                        IconButton(onClick = { onDeleteParticipant(p.id) }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
