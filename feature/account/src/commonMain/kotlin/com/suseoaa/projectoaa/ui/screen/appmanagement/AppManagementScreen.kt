package com.suseoaa.projectoaa.ui.screen.appmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.appmanagement.AppManagementViewModel
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthKnownScopes
import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.util.ToastManager
import org.koin.compose.viewmodel.koinViewModel

/** 正在编辑或新建的 OAuth 应用；existing 为 null 表示新建。 */
private data class AppEditingState(val existing: OaaApp?)

/**
 * OAuth 应用管理页：创建、编辑、停用与删除 OAuth 客户端。
 */
@Composable
fun AppManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<AppEditingState?>(null) }
    var deleting by remember { mutableStateOf<OaaApp?>(null) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            ToastManager.showToast(it)
            viewModel.clearMessage()
        }
    }

    AdaptivePageScaffold(
        title = "应用管理",
        onBack = onNavigateBack,
        sharedTransitionKey = "app_management_feature",
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新应用列表")
            }
            IconButton(onClick = { editing = AppEditingState(null) }) {
                Icon(Icons.Default.Add, contentDescription = "新建应用")
            }
        }
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            if (uiState.isLoading || uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!uiState.isLoading && uiState.apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "暂无 OAuth 应用",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { editing = AppEditingState(null) }) {
                            Text("新建应用")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.apps, key = { it.clientId }) { app ->
                        AppItemCard(
                            app = app,
                            onEdit = { editing = AppEditingState(app) },
                            onDelete = { deleting = app }
                        )
                    }
                }
            }
        }
    }

    editing?.let { current ->
        AppEditDialog(
            existing = current.existing,
            onDismiss = { editing = null },
            onConfirm = { clientId, name, redirectUris, scope, isActive ->
                if (current.existing == null) {
                    viewModel.createApp(clientId, name, redirectUris, scope)
                } else {
                    viewModel.updateApp(clientId, name, redirectUris, scope, isActive)
                }
                editing = null
            }
        )
    }

    deleting?.let { target ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { deleting = null },
            title = { Text("删除应用") },
            text = { Text("确定删除应用「${target.name}（${target.clientId}）」吗？删除后该应用将无法继续使用 OAuth 登录。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteApp(target.clientId)
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }

    uiState.createdSecretApp?.let { created ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = viewModel::dismissCreatedSecret,
            title = { Text("应用创建成功") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "请立即妥善保存 Client Secret，关闭弹窗后后端仅返回脱敏密钥。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Client ID：${created.clientId}", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = created.clientSecret,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(created.clientSecret))
                        ToastManager.showToast("已复制 Client Secret")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("复制密钥")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreatedSecret) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun AppItemCard(
    app: OaaApp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name.ifBlank { app.clientId },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Client ID：${app.clientId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!app.isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "已停用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑应用", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除应用", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (app.displaySecret.isNotBlank()) {
                Text(
                    text = "Secret：${app.displaySecret}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (app.redirectUris.isNotBlank()) {
                Text(
                    text = "回调地址：${app.redirectUris}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "授权范围：${app.scope.ifBlank { "无额外 scope" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppEditDialog(
    existing: OaaApp?,
    onDismiss: () -> Unit,
    onConfirm: (clientId: String, name: String, redirectUris: String, scope: String, isActive: Boolean) -> Unit
) {
    val isNew = existing == null
    var clientId by remember(existing) { mutableStateOf(existing?.clientId.orEmpty()) }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var redirectUris by remember(existing) { mutableStateOf(existing?.redirectUris.orEmpty()) }
    var scope by remember(existing) { mutableStateOf(existing?.scope.orEmpty()) }
    var isActive by remember(existing) { mutableStateOf(existing?.isActive ?: true) }

    val selectedScopes = remember(scope) {
        scope.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "新建 OAuth 应用" else "编辑 OAuth 应用") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client ID") },
                    placeholder = { Text("例如 suse_oj") },
                    enabled = isNew,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("应用名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = redirectUris,
                    onValueChange = { redirectUris = it },
                    label = { Text("回调地址") },
                    placeholder = { Text("回调地址（多个用空格分隔）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "常用 Scope 快捷勾选：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OAuthKnownScopes.all.forEach { known ->
                        val checked = known in selectedScopes
                        FilterChip(
                            selected = checked,
                            onClick = {
                                val next = if (checked) selectedScopes - known else selectedScopes + known
                                scope = next.joinToString(" ")
                            },
                            label = { Text(known) }
                        )
                    }
                }
                OutlinedTextField(
                    value = scope,
                    onValueChange = { scope = it },
                    label = { Text("Scope（空格分隔）") },
                    placeholder = { Text("例如 openid profile email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isNew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启用应用", modifier = Modifier.weight(1f))
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = clientId.isNotBlank() && name.isNotBlank() && redirectUris.isNotBlank(),
                onClick = { onConfirm(clientId, name, redirectUris, scope, isActive) }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
