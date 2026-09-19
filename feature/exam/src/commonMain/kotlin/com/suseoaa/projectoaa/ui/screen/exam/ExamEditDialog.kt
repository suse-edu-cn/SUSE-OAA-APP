package com.suseoaa.projectoaa.ui.screen.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.domain.exam.ExamUiItem
import com.suseoaa.projectoaa.ui.theme.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// 自建考试的编辑对话框，含日期与时间选择器。

/**
 * 考试编辑对话框
 */
@Composable
internal fun ExamEditDialog(
    exam: ExamUiItem,
    isAddMode: Boolean,
    isDarkTheme: Boolean,
    onSave: (ExamUiItem) -> Unit,
    onDelete: (ExamUiItem) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val errorColor = AlertRed // iOS 风格红

    // 编辑状态
    var courseName by remember { mutableStateOf(exam.courseName) }
    var location by remember { mutableStateOf(exam.location) }
    var credit by remember { mutableStateOf(exam.credit) }
    var examType by remember { mutableStateOf(exam.examType) }

    // 日期时间状态
    val now = remember { com.suseoaa.projectoaa.shared.util.OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var selectedDate by remember {
        mutableStateOf(parseExamDate(exam.time) ?: now.date)
    }
    var startHour by remember { mutableStateOf(parseExamStartHour(exam.time) ?: 9) }
    var startMinute by remember { mutableStateOf(parseExamStartMinute(exam.time) ?: 0) }
    var endHour by remember { mutableStateOf(parseExamEndHour(exam.time) ?: 11) }
    var endMinute by remember { mutableStateOf(parseExamEndMinute(exam.time) ?: 0) }

    // 显示日期选择器
    var showDatePicker by remember { mutableStateOf(false) }

    // 显示删除确认
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 格式化时间字符串 - 使用与API一致的格式: "2024-06-15(09:00-11:00)"
    fun formatTime(): String {
        val dateStr = "${selectedDate.year}-${
            selectedDate.monthNumber.toString().padStart(2, '0')
        }-${selectedDate.dayOfMonth.toString().padStart(2, '0')}"
        val startStr =
            "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}"
        val endStr =
            "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
        return "$dateStr($startStr-$endStr)"
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = surfaceColor,
            title = { Text("删除确认", color = textColor) },
            text = { Text("确定要删除这条考试信息吗？此操作无法撤销。", color = subtextColor) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(exam)
                    }
                ) {
                    Text("删除", color = errorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = subtextColor)
                }
            }
        )
    }

    // 日期选择器对话框
    if (showDatePicker) {
        ExamDatePicker(
            currentDate = selectedDate,
            isDarkTheme = isDarkTheme,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                text = if (isAddMode) "添加考试" else "编辑考试",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 课程名称（必填）
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedLabelColor = subtextColor,
                        cursorColor = primaryColor
                    )
                )

                // 考试日期（点击选择）
                Text(
                    text = "考试日期 *",
                    style = MaterialTheme.typography.labelMedium,
                    color = subtextColor
                )
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(
                        alpha = 0.05f
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedDate.year}年${selectedDate.monthNumber}月${selectedDate.dayOfMonth}日",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = subtextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 考试时间选择（开始-结束）
                Text(
                    text = "考试时间 *",
                    style = MaterialTheme.typography.labelMedium,
                    color = subtextColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 开始时间
                    TimePickerField(
                        hour = startHour,
                        minute = startMinute,
                        onTimeChange = { h, m -> startHour = h; startMinute = m },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )

                    Text("-", color = textColor, style = MaterialTheme.typography.titleMedium)

                    // 结束时间
                    TimePickerField(
                        hour = endHour,
                        minute = endMinute,
                        onTimeChange = { h, m -> endHour = h; endMinute = m },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 考试地点（必填）
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("考试地点 *") },
                    placeholder = { Text("如：A101教室", color = subtextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedLabelColor = subtextColor,
                        cursorColor = primaryColor
                    )
                )

                // 学分和考试类型（选填）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = credit,
                        onValueChange = { credit = it },
                        label = { Text("学分") },
                        placeholder = { Text("如：3", color = subtextColor) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedLabelColor = subtextColor,
                            cursorColor = primaryColor
                        )
                    )

                    OutlinedTextField(
                        value = examType,
                        onValueChange = { examType = it },
                        label = { Text("考试类型") },
                        placeholder = { Text("如：考试/考查", color = subtextColor) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedLabelColor = subtextColor,
                            cursorColor = primaryColor
                        )
                    )
                }

                // 学期信息（只读）
                Text(
                    text = "学期：${exam.yearSemester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedExam = exam.copy(
                        courseName = courseName.trim(),
                        time = formatTime(),
                        location = location.trim(),
                        credit = credit.trim(),
                        examType = examType.trim().ifEmpty { "考试" }
                    )
                    onSave(updatedExam)
                },
                enabled = courseName.isNotBlank() && location.isNotBlank()
            ) {
                Text(
                    "保存",
                    color = if (courseName.isNotBlank() && location.isNotBlank()) primaryColor else subtextColor
                )
            }
        },
        dismissButton = {
            Row {
                // 编辑模式下显示删除按钮
                if (!isAddMode) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = errorColor)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("取消", color = subtextColor)
                }
            }
        }
    )
}

// ============================================================================
// 日期时间选择器组件
// ============================================================================

/**
 * 考试日期选择器
 */
@Composable
internal fun ExamDatePicker(
    currentDate: LocalDate,
    isDarkTheme: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val selectedBgColor =
        if (isDarkTheme) NightBlue.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.1f)

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
                .widthIn(max = 400.dp)  // 限制最大宽度，防止平板上过大
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题
                Text(
                    "选择考试日期",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(Modifier.height(16.dp))

                // 选中日期显示
                Text(
                    "${selectedYear}年${monthNames[selectedMonth - 1]}${selectedDay}日",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
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
                            Icon(Icons.Default.ChevronLeft, null, tint = textColor)
                        }
                        Text(
                            "${selectedYear}年",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.ChevronRight, null, tint = textColor)
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
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = textColor)
                        }
                        Text(
                            monthNames[selectedMonth - 1],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        IconButton(onClick = {
                            if (selectedMonth < 12) selectedMonth++
                            else {
                                selectedMonth = 1; selectedYear++
                            }
                            val maxDay = daysInMonth(selectedYear, selectedMonth)
                            if (selectedDay > maxDay) selectedDay = maxDay
                        }) {
                            Icon(Icons.Default.ChevronRight, null, tint = textColor)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDayNames.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelMedium,
                                color = subtextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 日期网格
                val firstDay = firstDayOfMonth(selectedYear, selectedMonth)
                val daysCount = daysInMonth(selectedYear, selectedMonth)
                val totalCells = firstDay + daysCount
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val day = cellIndex - firstDay + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (day in 1..daysCount && day == selectedDay)
                                                selectedBgColor
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = day in 1..daysCount) {
                                            selectedDay = day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day in 1..daysCount) {
                                        Text(
                                            "$day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (day == selectedDay) primaryColor else textColor,
                                            fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal
                                        )
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
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = subtextColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(LocalDate(selectedYear, selectedMonth, selectedDay))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("确定", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 时间选择字段
 */
@Composable
internal fun TimePickerField(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey
    val bgColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            isDarkTheme = isDarkTheme,
            onTimeSelected = { h, m ->
                onTimeChange(h, m)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    Surface(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 时间选择器对话框
 */
@Composable
internal fun TimePickerDialog(
    hour: Int,
    minute: Int,
    isDarkTheme: Boolean,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkTheme) NightSurface else OxygenWhite
    val primaryColor = if (isDarkTheme) NightBlue else ElectricBlue
    val textColor = if (isDarkTheme) Color.White else InkBlack
    val subtextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else InkGrey

    var selectedHour by remember { mutableStateOf(hour) }
    var selectedMinute by remember { mutableStateOf(minute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "选择时间",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 时间选择器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 小时选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor)
                        }
                        Text(
                            selectedHour.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        IconButton(onClick = {
                            selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor)
                        }
                    }

                    Text(
                        ":",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 分钟选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor)
                        }
                        Text(
                            selectedMinute.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        IconButton(onClick = {
                            selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = subtextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTimeSelected(selectedHour, selectedMinute) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("确定", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 辅助解析函数
// ============================================================================
