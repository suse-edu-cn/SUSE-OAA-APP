package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
import com.suseoaa.projectoaa.ui.theme.*

// 「我的」页面复用的卡片与设置行控件。

@Composable
fun UserInfoCard(
    userInfo: com.suseoaa.projectoaa.shared.domain.model.person.PersonData?,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditInfo: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && userInfo != null) {
        EditInfoDialog(
            initialUsername = userInfo.username,
            initialName = userInfo.name,
            initialEmail = userInfo.email,
            onDismiss = { showEditDialog = false },
            onConfirm = { username, name, email ->
                onEditInfo(username, name, email)
                showEditDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像区域
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    // 头像主体
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onAvatarClick() }
                    ) {
                        if (userInfo?.avatar.isNullOrBlank()) {
                            // 无头像时显示默认图标
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            // 有头像时加载图片
                            AsyncImage(
                                model = userInfo.avatar,
                                contentDescription = "用户头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // 编辑图标提示 - 放在头像外层
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 用户信息 (可点击编辑)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userInfo?.name ?: "请登录",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = userInfo?.department ?: "暂未加入任何部门",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userInfo?.role ?: "未加入协会",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }

                // 退出登录按钮
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "退出登录",
                        tint = AlertRed
                    )
                }
            }
        }
    }
}

/**
 * 编辑信息对话框
 */
@Composable
fun EditInfoDialog(
    initialUsername: String,
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text("修改个人信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, name, email) },
                enabled = username.isNotBlank() && name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AppInfoCard(
    isUnlocked: Boolean = false,
    onSecretUnlocked: () -> Unit = {}
) {
    // 连续点击计数和时间追踪（仅在未解锁时使用）
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val resetTimeoutMs = 2000L // 2秒内需完成5次点击

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "青蟹",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 版本号 - 未解锁时可点击解锁隐藏功能
            Text(
                text = "版本 ${getAppVersionName()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (!isUnlocked) {
                    Modifier.clickable {
                        val currentTime =
                            com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
                        // 如果距上次点击超过超时时间，重置计数
                        if (currentTime - lastClickTime > resetTimeoutMs) {
                            clickCount = 1
                        } else {
                            clickCount++
                        }
                        lastClickTime = currentTime

                        // 达到5次点击，触发隐藏功能
                        if (clickCount >= 5) {
                            clickCount = 0
                            onSecretUnlocked()
                        }
                    }
                } else {
                    Modifier
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "四川轻化工大学开放原子开源协会",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
