package com.suseoaa.projectoaa.ui.screen.teachingplan.studyrequirement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.CollegeOption
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.MajorOption

// 修读要求的筛选面板与选择控件。

/**
 * 可折叠筛选区域
 */
@Composable
internal fun CollapsibleFilterSection(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean,
    hasResult: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 折叠头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "查询条件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isExpanded && (selectedGrade.isNotEmpty() || selectedCollege != null)) {
                            Text(
                                text = buildString {
                                    if (selectedGrade.isNotEmpty()) append("${selectedGrade}级")
                                    if (selectedCollege != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selectedCollege.name)
                                    }
                                    if (selectedMajor != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selectedMajor.name)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 可折叠内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 年级选择
                    ChipSelector(
                        label = "入学年份",
                        options = grades,
                        selectedOption = selectedGrade,
                        onSelect = onGradeSelect,
                        displayText = { grade -> if (grade.isNotEmpty()) "${grade}级" else "" }
                    )

                    // 学院选择
                    ChipSelector(
                        label = "学院",
                        options = colleges,
                        selectedOption = selectedCollege,
                        onSelect = onCollegeSelect,
                        displayText = { it.name }
                    )

                    // 专业选择
                    if (majors.isNotEmpty()) {
                        ChipSelector(
                            label = "专业",
                            options = majors,
                            selectedOption = selectedMajor,
                            onSelect = onMajorSelect,
                            displayText = { it.name }
                        )
                    } else if (selectedCollege != null) {
                        Text(
                            text = "加载专业中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 查询按钮
                    Button(
                        onClick = onQuery,
                        enabled = selectedGrade.isNotEmpty() &&
                                selectedCollege != null &&
                                selectedMajor != null &&
                                !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("查询")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 平板布局筛选面板
 */
@Composable
internal fun TabletFilterPanel(
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "查询条件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // 年级选择
            ChipSelector(
                label = "入学年份",
                options = grades,
                selectedOption = selectedGrade,
                onSelect = onGradeSelect,
                displayText = { grade -> if (grade.isNotEmpty()) "${grade}级" else "" }
            )

            // 学院选择
            ChipSelector(
                label = "学院",
                options = colleges,
                selectedOption = selectedCollege,
                onSelect = onCollegeSelect,
                displayText = { it.name }
            )

            // 专业选择
            if (majors.isNotEmpty()) {
                ChipSelector(
                    label = "专业",
                    options = majors,
                    selectedOption = selectedMajor,
                    onSelect = onMajorSelect,
                    displayText = { it.name }
                )
            } else if (selectedCollege != null) {
                Text(
                    text = "加载专业中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 查询按钮
            Button(
                onClick = onQuery,
                enabled = selectedGrade.isNotEmpty() &&
                        selectedCollege != null &&
                        selectedMajor != null &&
                        !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询")
                }
            }
        }
    }
}

/**
 * 筛选区域 (保留向后兼容)
 */
@Composable
internal fun FilterSection(
    grades: List<String>,
    colleges: List<CollegeOption>,
    majors: List<MajorOption>,
    selectedGrade: String,
    selectedCollege: CollegeOption?,
    selectedMajor: MajorOption?,
    onGradeSelect: (String) -> Unit,
    onCollegeSelect: (CollegeOption) -> Unit,
    onMajorSelect: (MajorOption) -> Unit,
    onQuery: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 年级选择
            ChipSelector(
                label = "入学年份",
                options = grades,
                selectedOption = selectedGrade,
                onSelect = onGradeSelect,
                displayText = { grade -> if (grade.isNotEmpty()) "${grade}级" else "" }
            )

            // 学院选择
            ChipSelector(
                label = "学院",
                options = colleges,
                selectedOption = selectedCollege,
                onSelect = onCollegeSelect,
                displayText = { it.name }
            )

            // 专业选择
            if (majors.isNotEmpty()) {
                ChipSelector(
                    label = "专业",
                    options = majors,
                    selectedOption = selectedMajor,
                    onSelect = onMajorSelect,
                    displayText = { it.name }
                )
            }

            // 查询按钮
            Button(
                onClick = onQuery,
                enabled = selectedGrade.isNotEmpty() &&
                        selectedCollege != null &&
                        selectedMajor != null &&
                        !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询")
                }
            }
        }
    }
}

/**
 * iOS 风格选项卡片选择器 - 支持泛型类型
 */
@Composable
private fun <T> ChipSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(options.size) { index ->
                val option = options[index]
                val isSelected = option == selectedOption
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(200),
                    label = "chipBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "chipContent"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 0.dp else 1.dp,
                    animationSpec = tween(200),
                    label = "chipBorder"
                )

                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor,
                    modifier = Modifier
                        .then(
                            if (!isSelected) Modifier.border(
                                width = borderWidth,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            ) else Modifier
                        )
                ) {
                    Text(
                        text = displayText(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 可搜索的选项选择器 - 用于学院/专业等大量选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SearchableChipSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String,
    placeholder: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isEmpty()) options
        else options.filter { displayText(it).contains(searchQuery, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        // 已选中的选项显示
        if (selectedOption != null) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayText(selectedOption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // 未选中时显示点击选择按钮
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的选项列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // 搜索框
                    if (options.size > 5) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, "清除")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // 选项网格
                    val maxHeight = if (filteredOptions.size > 6) 200.dp else Dp.Unspecified
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredOptions.take(20).forEach { option ->
                            val isSelected = option == selectedOption
                            Surface(
                                onClick = {
                                    onSelect(option)
                                    isExpanded = false
                                    searchQuery = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayText(option),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredOptions.size > 20) {
                            Text(
                                text = "还有 ${filteredOptions.size - 20} 个选项，请搜索缩小范围",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (filteredOptions.isEmpty()) {
                            Text(
                                text = "未找到匹配项",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 通用下拉选择器 (保留兼容性)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    displayText: (T?) -> String,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = displayText(selectedOption),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayText(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
