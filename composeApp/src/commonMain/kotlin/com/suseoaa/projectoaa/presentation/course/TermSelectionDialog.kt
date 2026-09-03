package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.*
import kotlin.math.abs

/**
 * 学期选择对话框及其滚轮选择器。
 */

/**
 * 学期选择对话框
 */
@Composable
internal fun TermSelectionDialog(
    termOptions: List<TermOption>,
    currentXnm: String,
    currentXqm: String,
    onTermSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val yearOptions = remember(termOptions) {
            termOptions.map { it.xnm }.distinct().sortedDescending()
        }

        var selectedYear by remember(yearOptions, currentXnm) {
            mutableStateOf(
                yearOptions.firstOrNull { it == currentXnm }
                    ?: yearOptions.firstOrNull().orEmpty()
            )
        }

        val semesterOptions = remember(termOptions, selectedYear) {
            termOptions
                .asSequence()
                .filter { selectedYear.isBlank() || it.xnm == selectedYear }
                .map { it.xqm }
                .distinct()
                .sortedBy { code ->
                    when (code) {
                        "3" -> 0
                        "12" -> 1
                        else -> 2
                    }
                }
                .toList()
        }

        var selectedSemester by remember(currentXqm) {
            mutableStateOf(currentXqm)
        }

        LaunchedEffect(semesterOptions) {
            if (semesterOptions.isEmpty()) {
                selectedSemester = ""
            } else if (selectedSemester !in semesterOptions) {
                selectedSemester = semesterOptions.first()
            }
        }

        val selectedLabel = remember(selectedYear, selectedSemester) {
            if (selectedYear.isNotBlank() && selectedSemester.isNotBlank()) {
                formatTermLabel(selectedYear, selectedSemester)
            } else {
                "暂无可选学期"
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "选择学期",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (termOptions.isEmpty()) {
                    Text(
                        text = "暂无可选学期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "学年",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            TermWheelPicker(
                                options = yearOptions,
                                selectedValue = selectedYear,
                                optionLabel = { formatAcademicYearLabel(it) },
                                onSelected = { year ->
                                    selectedYear = year
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "学期",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            TermWheelPicker(
                                options = semesterOptions,
                                selectedValue = selectedSemester,
                                optionLabel = { semesterLabel(it) },
                                onSelected = { semester ->
                                    selectedSemester = semester
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        enabled = termOptions.isNotEmpty() && selectedYear.isNotBlank() && selectedSemester.isNotBlank(),
                        onClick = {
                            val exactMatch = termOptions.firstOrNull {
                                it.xnm == selectedYear && it.xqm == selectedSemester
                            }
                            val fallback = termOptions.firstOrNull { it.xnm == selectedYear }
                                ?: termOptions.firstOrNull()
                            val target = exactMatch ?: fallback ?: return@Button
                            onTermSelected(target.xnm, target.xqm)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun TermWheelPicker(
    options: List<String>,
    selectedValue: String,
    optionLabel: (String) -> String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCount = 5
    val sidePaddingCount = visibleCount / 2
    val itemHeight = 56.dp
    val listState = rememberLazyListState()
    val centeredIndex by remember(listState, sidePaddingCount, options.size) {
        derivedStateOf {
            calculateCenteredWheelIndex(
                listState = listState,
                sidePaddingCount = sidePaddingCount,
                optionCount = options.size
            )
        }
    }

    LaunchedEffect(options, selectedValue) {
        if (options.isEmpty() || listState.isScrollInProgress) return@LaunchedEffect
        val targetIndex = options.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
        val currentIndex = calculateCenteredWheelIndex(
            listState = listState,
            sidePaddingCount = sidePaddingCount,
            optionCount = options.size
        )
        if (currentIndex != targetIndex) {
            // 对于包含顶部/底部占位项的轮盘，scrollToItem 的目标就是 optionIndex。
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(centeredIndex, options) {
        if (options.isNotEmpty()) {
            val target = options[centeredIndex]
            if (target != selectedValue) {
                onSelected(target)
            }
        }
    }

    LaunchedEffect(options, listState) {
        if (options.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress && options.isNotEmpty()) {
                val targetIndex = calculateCenteredWheelIndex(
                    listState = listState,
                    sidePaddingCount = sidePaddingCount,
                    optionCount = options.size
                )
                val shouldSnap = listState.firstVisibleItemIndex != targetIndex ||
                        listState.firstVisibleItemScrollOffset != 0
                if (shouldSnap) {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = options.size > 1
        ) {
            items(sidePaddingCount) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                )
            }

            items(options.size) { index ->
                val isSelected = index == centeredIndex
                val textStyle = if (isSelected) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyLarge
                }

                Text(
                    text = optionLabel(options[index]),
                    style = textStyle,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(horizontal = 20.dp)
                )
            }

            items(sidePaddingCount) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
        ) {}

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(itemHeight * 1.4f)
                .blur(10.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(itemHeight * 1.4f)
                .blur(10.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

private fun calculateCenteredWheelIndex(
    listState: LazyListState,
    sidePaddingCount: Int,
    optionCount: Int
): Int {
    if (optionCount <= 0) return 0
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return 0

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
        abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
    }

    return ((centeredItem?.index ?: sidePaddingCount) - sidePaddingCount)
        .coerceIn(0, optionCount - 1)
}

private fun formatAcademicYearLabel(xnm: String): String {
    val year = xnm.toIntOrNull()
    return if (year != null) {
        "$year-${year + 1} 学年"
    } else {
        "${xnm} 学年"
    }
}

private fun semesterLabel(xqm: String): String = when (xqm) {
    "3" -> "上学期"
    "12" -> "下学期"
    else -> "学期$xqm"
}

private fun formatTermLabel(xnm: String, xqm: String): String {
    val year = xnm.toIntOrNull()
    val yearLabel = if (year != null) "$year-${year + 1}学年" else "${xnm}学年"
    val semesterText = when (xqm) {
        "3" -> "第1学期"
        "12" -> "第2学期"
        else -> "学期$xqm"
    }
    return "$yearLabel $semesterText"
}
