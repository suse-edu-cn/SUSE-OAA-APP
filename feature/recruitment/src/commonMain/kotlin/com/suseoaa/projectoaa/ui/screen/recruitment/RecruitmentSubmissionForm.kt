package com.suseoaa.projectoaa.ui.screen.recruitment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.recruitment.RecruitmentApplication

// 报名表单及其输入控件。

@Composable
internal fun SubmissionForm(
    application: RecruitmentApplication,
    isCompact: Boolean,
    hasExistingApplication: Boolean,
    pickedAvatar: ByteArray?,
    onPickAvatar: () -> Unit,
    onUpdateField: ((RecruitmentApplication) -> RecruitmentApplication) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (hasExistingApplication) "修改我的申请表" else "填写申请表",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            AvatarSection(
                avatarUrl = application.avatarUrl,
                pickedAvatar = pickedAvatar,
                isEditable = true,
                onPickAvatar = onPickAvatar
            )

            RecruitmentTextField(
                label = "姓名（系统自动识别）",
                value = application.name,
                isEditable = false,
                onValueChange = {}
            )

            if (isCompact) {
                ApplicationFieldsColumn(
                    application = application,
                    isEditable = true,
                    onUpdateField = onUpdateField
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ApplicationLeftFields(
                            application = application,
                            isEditable = true,
                            onUpdateField = onUpdateField
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ApplicationRightFields(
                            application = application,
                            isEditable = true,
                            onUpdateField = onUpdateField
                        )
                    }
                }
            }

            AdjustmentSwitch(
                isChecked = application.adjustment == 1,
                enabled = true,
                onCheckedChange = { checked ->
                    onUpdateField { it.copy(adjustment = if (checked) 1 else 0) }
                }
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (hasExistingApplication) "保存修改" else "提交申请",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
internal fun RecruitmentTextField(
    label: String,
    value: String,
    isEditable: Boolean,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = !isEditable,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    )
}

@Composable
internal fun AdjustmentSwitch(
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("是否服从调剂", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (isChecked) "当前：服从调剂（1）" else "当前：不服从调剂（0）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
