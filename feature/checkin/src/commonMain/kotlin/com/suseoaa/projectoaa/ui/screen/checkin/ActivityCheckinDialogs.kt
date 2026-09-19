package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
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

// 创建任务对话框与签到成功浮层。

/**
 * 主机对话框
 */
@Composable
fun HostTaskDialog(
    isBroadcasting: Boolean,
    initialActivityName: String = "",
    initialHostName: String = "协会成员",
    onStart: (String, String, Long, Long) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    var activityName by remember { mutableStateOf(initialActivityName) }
    var hostName by remember { mutableStateOf(initialHostName) }
    var durationMinutes by remember { mutableStateOf(30f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBroadcasting) "正在发布中" else if (initialActivityName.isNotEmpty()) "重新开启签到" else "创建签到任务") },
        text = {
            if (isBroadcasting) {
                Text("您正在广播签到任务，附近的同学可以搜索到您并完成签到。")
            } else {
                Column {
                    OutlinedTextField(
                        value = activityName,
                        onValueChange = { activityName = it },
                        label = { Text("活动名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hostName,
                        onValueChange = { hostName = it },
                        label = { Text("主持人姓名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "签到时长: ${durationMinutes.toInt()} 分钟",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        valueRange = 5f..120f,
                        steps = 23
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isBroadcasting) {
                        onStop()
                    } else {
                        val start =
                            com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
                        val end = start + (durationMinutes.toInt() * 60 * 1000)
                        onStart(activityName, hostName, start, end)
                    }
                },
                enabled = isBroadcasting || (activityName.isNotBlank() && hostName.isNotBlank())
            ) {
                Text(if (isBroadcasting) "停止发布" else "开始发布")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 签到成功覆盖层
 */
@Composable
fun CheckinSuccessOverlay(
    taskName: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "签到成功",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "您已完成「$taskName」的签到",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
