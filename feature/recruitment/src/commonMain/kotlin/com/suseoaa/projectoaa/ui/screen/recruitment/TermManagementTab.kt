package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.TermDraft
import com.suseoaa.projectoaa.presentation.recruitment.TermManagementUiState
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Interviewer
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermTypes
import com.suseoaa.projectoaa.ui.component.common.OptionDropdown

// 「周期管理」Tab：筛选 / 新建 / 修改 / 删除周期，管理周期下的面试官。

/** 正在编辑的周期；existing 为 null 表示新建。 */
private data class TermEditing(val existing: Term?)
private const val ALL_TYPES = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TermManagementTab(
    uiState: TermManagementUiState,
    onUpdateFilters: (year: String?, type: String?, autoReload: Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSaveTerm: (Term?, TermDraft) -> Unit,
    onDeleteTerm: (Term) -> Unit,
    onOpenInterviewerManager: (Term) -> Unit,
    onSearchUsers: (String) -> Unit,
    onAddInterviewers: (Term, List<UserListItem>, String) -> Unit,
    onUpdateInterviewerRemark: (Term, Interviewer, String) -> Unit,
    onRemoveInterviewer: (Term, Interviewer) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<TermEditing?>(null) }
    var deleting by remember { mutableStateOf<Term?>(null) }
    var managingInterviewersFor by remember { mutableStateOf<Term?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading || uiState.isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 顶部年份与类型筛选栏（对应 GET /term/list?year=&type=）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.filterYear,
                onValueChange = { v ->
                    onUpdateFilters(v.filter { it.isDigit() }.take(4), null, false)
                },
                label = { Text("年份") },
                placeholder = { Text("全部") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onRefresh() }),
                modifier = Modifier.weight(1f)
            )
            OptionDropdown(
                label = "类型",
                options = listOf(ALL_TYPES) + TermTypes.all,
                selected = uiState.filterType,
                onSelect = { onUpdateFilters(null, it, true) },
                optionLabel = { it.ifEmpty { "全部类型" } },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "查询")
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.terms.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Button(onClick = { editing = TermEditing(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("新建周期", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                if (!uiState.isLoading && uiState.terms.isEmpty()) {
                    item {
                        EmptyHint(text = "当前筛选条件下暂无周期", onRefresh = onRefresh)
                    }
                }
                items(uiState.terms, key = { it.resolvedId }) { term ->
                    TermCard(
                        term = term,
                        onEdit = { editing = TermEditing(term) },
                        onDelete = { deleting = term },
                        onManageInterviewers = {
                            managingInterviewersFor = term
                            onOpenInterviewerManager(term)
                        }
                    )
                }
            }
        }
    }

    editing?.let { current ->
        TermDialog(
            existing = current.existing,
            onDismiss = { editing = null },
            onConfirm = { draft ->
                onSaveTerm(current.existing, draft)
                editing = null
            }
        )
    }

    deleting?.let { term ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { deleting = null },
            title = { Text("删除周期") },
            text = { Text("确定删除「${term.displayTitle()}」吗？该周期下的申请可能一并失效。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTerm(term)
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }

    managingInterviewersFor?.let { term ->
        InterviewerManagementDialog(
            term = term,
            uiState = uiState,
            onSearch = onSearchUsers,
            onAddInterviewers = { users, remark -> onAddInterviewers(term, users, remark) },
            onUpdateRemark = { interviewer, remark -> onUpdateInterviewerRemark(term, interviewer, remark) },
            onRemoveInterviewer = { interviewer -> onRemoveInterviewer(term, interviewer) },
            onDismiss = { managingInterviewersFor = null }
        )
    }
}

@Composable
private fun TermCard(term: Term, onEdit: () -> Unit, onDelete: () -> Unit, onManageInterviewers: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(term.displayTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${term.year} · ${term.type}", style = MaterialTheme.typography.bodySmall)
            Text(
                "填写：${term.editPeriod.startAt.take(10)} ~ ${term.editPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "公示：${term.queryPeriod.startAt.take(10)} ~ ${term.queryPeriod.endAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (term.isExecuted) {
                Text(
                    "已执行录取${term.executedAt?.let { "（${it.take(10)}）" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onManageInterviewers) { Text("面试官管理") }
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TermDialog(existing: Term?, onDismiss: () -> Unit, onConfirm: (TermDraft) -> Unit) {
    var draft by remember { mutableStateOf(existing?.let { TermDraft.from(it) } ?: TermDraft(type = TermTypes.RECRUITMENT)) }
    val isNew = existing == null

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "新建周期" else "编辑周期") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 更新接口不接受年份和类型，编辑时只读
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.year,
                        onValueChange = { v -> draft = draft.copy(year = v.filter { it.isDigit() }.take(4)) },
                        label = { Text("年份") },
                        enabled = isNew,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OptionDropdown(
                        label = "类型",
                        options = TermTypes.all,
                        selected = draft.type.ifBlank { null },
                        onSelect = { draft = draft.copy(type = it) },
                        optionLabel = { it },
                        enabled = isNew,
                        modifier = Modifier.weight(1f)
                    )
                }
                DialogField("标题", draft.title) { draft = draft.copy(title = it) }
                Text("日期格式：2026-09-01", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogField("填写开始", draft.editStart, Modifier.weight(1f)) { draft = draft.copy(editStart = it) }
                    DialogField("填写结束", draft.editEnd, Modifier.weight(1f)) { draft = draft.copy(editEnd = it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogField("公示开始", draft.queryStart, Modifier.weight(1f)) { draft = draft.copy(queryStart = it) }
                    DialogField("公示结束", draft.queryEnd, Modifier.weight(1f)) { draft = draft.copy(queryEnd = it) }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(draft) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DialogField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

/**
 * 面试官管理弹窗：
 * - 「已指派」Tab：展示 GET /interviewer/list 结果，支持修改备注（/interviewer/update）与移除（/interviewer/delete）；
 * - 「添加成员」Tab：支持从全部可见成员中快速过滤并批量指派（/interviewer/create）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterviewerManagementDialog(
    term: Term,
    uiState: TermManagementUiState,
    onSearch: (String) -> Unit,
    onAddInterviewers: (List<UserListItem>, String) -> Unit,
    onUpdateRemark: (Interviewer, String) -> Unit,
    onRemoveInterviewer: (Interviewer) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var keyword by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Map<Int, UserListItem>>(emptyMap()) }
    var editingInterviewer by remember { mutableStateOf<Interviewer?>(null) }
    var removingInterviewer by remember { mutableStateOf<Interviewer?>(null) }

    val existingUserIds = remember(uiState.interviewers) {
        uiState.interviewers.mapNotNull { it.userId.takeIf { id -> id > 0 } }.toSet()
    }
    val existingSignatures = remember(uiState.interviewers) {
        uiState.interviewers
            .filter { it.name.isNotBlank() }
            .map { Triple(it.name.trim(), it.resolvedDepartment.trim(), it.role.trim()) }
            .toSet()
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("面试官管理 · ${term.displayTitle()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text("已指派 (${uiState.interviewers.size})") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text("添加成员") }
                    )
                }

                if (activeTab == 0) {
                    if (uiState.isLoadingInterviewers) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.interviewers.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "暂未指派面试官",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = { activeTab = 1 }) {
                                Text("去添加面试官")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = uiState.interviewers,
                                key = { item -> if (item.resolvedId != 0) item.resolvedId else item.hashCode() }
                            ) { interviewer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = interviewer.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val orgLine = listOf(interviewer.resolvedDepartment, interviewer.role)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · ")
                                        if (orgLine.isNotBlank()) {
                                            Text(
                                                text = orgLine,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (interviewer.remark.isNotBlank()) {
                                            Text(
                                                text = "备注：${interviewer.remark}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { editingInterviewer = interviewer },
                                        enabled = !uiState.isSaving
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "修改备注",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { removingInterviewer = interviewer },
                                        enabled = !uiState.isSaving
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "移除面试官",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = {
                                keyword = it
                                onSearch(it)
                            },
                            label = { Text("姓名 / 学号 / 部门筛选") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { onSearch(keyword) }) { Text("搜索") }
                    }
                    if (uiState.isSearchingUsers) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                        val shown = (selected.values + uiState.userResults)
                            .filter { it.isGraduated != true }.distinctBy { it.userId }
                        items(shown, key = { it.userId }) { user ->
                            val userName = user.name.ifBlank { user.username }.trim()
                            val alreadyAssigned = user.userId in existingUserIds ||
                                (userName.isNotEmpty() &&
                                    Triple(userName, user.department.trim(), user.role.trim()) in existingSignatures)
                            val checked = user.userId in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyAssigned) {
                                        selected = if (checked) {
                                            selected - user.userId
                                        } else {
                                            selected + (user.userId to user)
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked || alreadyAssigned,
                                    onCheckedChange = null,
                                    enabled = !alreadyAssigned
                                )
                                Column(modifier = Modifier.padding(start = 6.dp).weight(1f)) {
                                    Text(
                                        text = "${user.name.ifBlank { user.username }}（${user.department} · ${user.role}）",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (alreadyAssigned) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (alreadyAssigned) {
                                        Text(
                                            text = "已是本周期面试官",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("统一备注（留空则默认成员姓名）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (activeTab == 1) {
                Button(
                    enabled = selected.isNotEmpty() && !uiState.isSaving,
                    onClick = {
                        onAddInterviewers(selected.values.toList(), remark.trim())
                        selected = emptyMap()
                        remark = ""
                        activeTab = 0
                    }
                ) {
                    Text("添加 ${selected.size} 人")
                }
            } else {
                Button(onClick = { activeTab = 1 }) {
                    Text("添加面试官")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    editingInterviewer?.let { target ->
        var editedRemark by remember(target) { mutableStateOf(target.remark) }
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { editingInterviewer = null },
            title = { Text("修改面试官备注") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.displayName, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = editedRemark,
                        onValueChange = { editedRemark = it },
                        label = { Text("备注") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateRemark(target, editedRemark)
                        editingInterviewer = null
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingInterviewer = null }) { Text("取消") }
            }
        )
    }

    removingInterviewer?.let { target ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { removingInterviewer = null },
            title = { Text("移除面试官") },
            text = { Text("确定移除「${target.displayName}」的本周期面试官权限吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveInterviewer(target)
                        removingInterviewer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { removingInterviewer = null }) { Text("取消") }
            }
        )
    }
}
