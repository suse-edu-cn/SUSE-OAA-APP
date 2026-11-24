package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.recruitment.RecruitmentReviewUiState

/** 导出面试结果字段选择弹窗（对应 GET /interviewer/result/fields 与 /interviewer/result/export）。 */
@Composable
internal fun ReviewExportDialog(
    uiState: RecruitmentReviewUiState,
    onReloadFields: () -> Unit,
    onExport: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val config = uiState.exportFieldsConfig
    var selectedIndices by remember(config) {
        mutableStateOf(config?.fields?.map { it.index }?.toSet().orEmpty())
    }

    LaunchedEffect(config) {
        if (config != null) {
            selectedIndices = config.fields.map { it.index }.toSet()
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("导出面试结果") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "将按当前筛选条件导出 Excel 表格，可勾选需要包含的列：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.isLoadingExportFields) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (config == null || config.fields.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "未能加载导出字段配置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onReloadFields) {
                            Text("重新加载")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已选 ${selectedIndices.size} / ${config.fields.size} 项",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { selectedIndices = config.fields.map { it.index }.toSet() }
                            ) {
                                Text("全选")
                            }
                            TextButton(onClick = { selectedIndices = emptySet() }) {
                                Text("清空")
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(config.fields, key = { it.index }) { field ->
                            val checked = field.index in selectedIndices
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndices = if (checked) {
                                            selectedIndices - field.index
                                        } else {
                                            selectedIndices + field.index
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedIndices = if (isChecked) {
                                            selectedIndices + field.index
                                        } else {
                                            selectedIndices - field.index
                                        }
                                    }
                                )
                                Text(
                                    text = field.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !uiState.isLoadingExportFields &&
                    !uiState.isExporting &&
                    config != null &&
                    selectedIndices.isNotEmpty(),
                onClick = { onExport(selectedIndices) }
            ) {
                Text(if (uiState.isExporting) "导出中..." else "导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
