package com.suseoaa.projectoaa.student.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
// [解耦] 移除了 LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
// [解耦] 移除了 WallpaperManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

// 统一的圆角风格
val InputFieldShape = RoundedCornerShape(12.dp)

@Composable
fun LargeSelectionButton(text: String, onClick: () -> Unit, isSecondary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        colors = if (isSecondary) ButtonDefaults.filledTonalButtonColors()
        else ButtonDefaults.buttonColors()
    ) {
        Text(text = text, fontSize = 22.sp)
    }
}

@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    // [解耦] 添加 onThemeSelected 回调
    onThemeSelected: (OaaThemeConfig) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "选择历史主题",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ThemeManager.themeList) { theme ->
                        // [解耦] 将回调传递下去
                        ThemeOptionItem(theme, onThemeSelected)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    }
}

@Composable
fun ThemeOptionItem(
    theme: OaaThemeConfig,
    // [解耦] 接收一个回调，而不是 onDismiss
    onThemeSelected: (OaaThemeConfig) -> Unit
) {
    val isSelected = ThemeManager.currentTheme == theme
    // [解耦] 移除了 context

    Surface(
        onClick = {
            // [解耦] 只报告事件，不执行逻辑
            onThemeSelected(theme)
        },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(theme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                theme.name, fontWeight = if (isSelected) FontWeight.Bold
                else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CompactTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false,
    error: String? = null,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = InputFieldShape,
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default,
        isError = error != null,
        supportingText = {
            if (error != null) Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingIcon = {
            if (error != null) Icon(
                Icons.Default.Info,
                null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}

@Composable
fun CompactTextArea(
    value: String,
    label: String,
    error: String? = null,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (error != null) 135.dp else 115.dp),
        maxLines = 4,
        shape = InputFieldShape,
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        isError = error != null,
        supportingText = {
            if (error != null) Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

@Composable
fun CompactDateTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = {},
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        enabled = false,
        shape = InputFieldShape,
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        isError = error != null,
        supportingText = {
            if (error != null) Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingIcon = {
            if (error != null) Icon(
                Icons.Default.Info,
                null,
                tint = MaterialTheme.colorScheme.error
            ) else Icon(Icons.Default.DateRange, null)
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline,
            disabledLabelColor = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDropdownMenu(
    value: String,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }


    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = {
            if (enabled) {
                expanded = !expanded
            }
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },

            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .then(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled
                    ) {
                        expanded = !expanded
                    }
                ),

            enabled = enabled,

            shape = InputFieldShape,
            isError = error != null,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            supportingText = {
                if (error != null) Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.3f
                ),
                disabledLabelColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 下拉菜单内容
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 15.sp) },
                    onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

fun Modifier.scale(scale: Float) =
    this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))