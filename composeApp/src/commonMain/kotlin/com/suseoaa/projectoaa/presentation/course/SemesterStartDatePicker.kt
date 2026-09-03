package com.suseoaa.projectoaa.presentation.course

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.*

/**
 * 学期起始日期选择器。
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SemesterStartDatePicker(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentDate.year) }
    var selectedMonth by remember { mutableStateOf(currentDate.monthNumber) }
    var selectedDay by remember { mutableStateOf(currentDate.dayOfMonth) }

    val monthNames = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )
    val weekDayNames = listOf("一", "二", "三", "四", "五", "六", "日")

    // 计算某月的天数
    fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            else -> 30
        }
    }

    // 获取某月第一天是星期几 (0=周一, 6=周日)
    fun firstDayOfMonth(year: Int, month: Int): Int {
        val date = LocalDate(year, month, 1)
        return date.dayOfWeek.ordinal
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题
                Text(
                    "选择开学日期",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    "选择本学期第一周的周一",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(20.dp))

                // 选中日期显示
                Text(
                    "${selectedYear}年${monthNames[selectedMonth - 1]}${selectedDay}日",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 年月选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 年份选择
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(
                                Icons.Default.ArrowDropDown, null,
                                modifier = Modifier.scale(-1f, 1f).rotate(90f)
                            )
                        }
                        Text(
                            "${selectedYear}年",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(
                                Icons.Default.ArrowDropDown, null,
                                modifier = Modifier.rotate(-90f)
                            )
                        }
                    }

                    // 月份选择
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth > 1) selectedMonth--
                            else {
                                selectedMonth = 12; selectedYear--
                            }
                            // 调整日期
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(
                                Icons.Default.ArrowDropDown, null,
                                modifier = Modifier.scale(-1f, 1f).rotate(90f)
                            )
                        }
                        Text(
                            monthNames[selectedMonth - 1],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {
                            if (selectedMonth < 12) selectedMonth++
                            else {
                                selectedMonth = 1; selectedYear++
                            }
                            // 调整日期
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(
                                Icons.Default.ArrowDropDown, null,
                                modifier = Modifier.rotate(-90f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDayNames.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 日期网格
                val daysInCurrentMonth = daysInMonth(selectedYear, selectedMonth)
                val firstDay = firstDayOfMonth(selectedYear, selectedMonth)
                val totalCells = ((daysInCurrentMonth + firstDay + 6) / 7) * 7

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (week in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayOfWeek in 0..6) {
                                val cellIndex = week * 7 + dayOfWeek
                                val dayNumber = cellIndex - firstDay + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .then(
                                            if (dayNumber in 1..daysInCurrentMonth) {
                                                Modifier.clickable { selectedDay = dayNumber }
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNumber in 1..daysInCurrentMonth) {
                                        val isSelected = dayNumber == selectedDay
                                        val isToday = selectedYear == currentDate.year &&
                                                selectedMonth == currentDate.monthNumber &&
                                                dayNumber == currentDate.dayOfMonth

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 按钮
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
                        onClick = {
                            val date = LocalDate(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(date)
                            onDismiss()
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
