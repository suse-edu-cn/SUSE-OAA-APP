package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import kotlinx.datetime.*

/**
 * 课表相关的账号类对话框：账号选择、冲突账号筛选与登录。
 */

// ==================== 对话框组件 ====================

@Composable
internal fun AccountSelectionDialog(
    accounts: List<CourseAccountEntity>,
    currentId: String,
    onSelect: (CourseAccountEntity) -> Unit,
    onDelete: (CourseAccountEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 6.dp,
        onDismissRequest = onDismiss,
        title = {
            Text(
                "教务系统账号管理",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    "这些是您保存的教务系统账号，用于导入课表。\n与软件登录账号无关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn {
                    items(accounts) { acc ->
                        var showPassword by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(acc) },
                            colors = if (acc.studentId == currentId) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${acc.name} - ${acc.className}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (acc.studentId == currentId) {
                                            Text(
                                                "当前选中",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDelete(acc) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            "删除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.clickable { showPassword = !showPassword }
                                ) {
                                    Text(
                                        "学号: ${acc.studentId}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "密码: ${if (showPassword) acc.password else "******"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("添加新账号")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
internal fun OverlapAccountSelectionDialog(
    accounts: List<CourseAccountEntity>,
    currentId: String,
    selectedIds: Set<String>,
    count: OverlapLegendCount,
    selectedFilter: OverlapDisplayFilter,
    onlyShowOverlap: Boolean,
    onFilterSelected: (OverlapDisplayFilter) -> Unit,
    onOnlyShowOverlapChange: (Boolean) -> Unit,
    onSelectedIdsChange: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val filters = remember {
        listOf(
            OverlapDisplayFilter.ALL,
            OverlapDisplayFilter.NO_OVERLAP,
            OverlapDisplayFilter.OVERLAP,
            OverlapDisplayFilter.PARTIAL_OVERLAP
        )
    }
    val countByFilter = remember(count) {
        mapOf(
            OverlapDisplayFilter.ALL to count.total,
            OverlapDisplayFilter.NO_OVERLAP to count.noOverlap,
            OverlapDisplayFilter.OVERLAP to count.overlap,
            OverlapDisplayFilter.PARTIAL_OVERLAP to count.partialOverlap
        )
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = {
            Text(
                "重课查询账号",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    "在这里配置重课查询：显示规则 + 参与账号。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "显示规则",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "仅看与当前账号重合",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = onlyShowOverlap,
                        onCheckedChange = onOnlyShowOverlapChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val label = "${overlapFilterLabel(filter)} ${countByFilter[filter] ?: 0}"
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(overlapFilterColor(filter))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = overlapFilterColor(filter).copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "参与账号",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accounts) { account ->
                        val isCurrent = account.studentId == currentId
                        val checked = account.studentId in selectedIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (checked) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.5f
                                    )
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                                .clickable {
                                    val updated = if (checked) {
                                        selectedIds - account.studentId
                                    } else {
                                        selectedIds + account.studentId
                                    }
                                    onSelectedIdsChange(updated)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { checkedState ->
                                    val updated = if (checkedState) {
                                        selectedIds + account.studentId
                                    } else {
                                        selectedIds - account.studentId
                                    }
                                    onSelectedIdsChange(updated)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${account.name} (${account.studentId})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isCurrent) "当前查看账号" else account.className,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("关闭")
            }
        }
    )
}

@Composable
internal fun LoginDialog(
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text("导入课表") },
        text = {
            Column {
                Text(
                    "请输入教务系统账号（学号和密码）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("教务系统密码") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, password) },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
