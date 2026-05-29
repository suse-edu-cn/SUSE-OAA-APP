package com.suseoaa.projectoaa.composeapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import java.util.Calendar

class WeeklyScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")

    // Define 5 distinct theme colors for seamless drawables
    enum class CourseTheme(val textHex: Long, val baseName: String) {
        BLUE(0xFF1565C0, "bg_course_blue"),
        GREEN(0xFF2E7D32, "bg_course_green"),
        PINK(0xFFAD1457, "bg_course_pink"),
        ORANGE(0xFFEF6C00, "bg_course_orange"),
        PURPLE(0xFF6A1B9A, "bg_course_purple")
    }

    private fun getCourseTheme(name: String): CourseTheme {
        val themes = CourseTheme.values()
        val index = Math.abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weeklyMap = WidgetDataFetcher.getWeeklyCourses()
        val currentWeek = WidgetDataFetcher.getCurrentWeek()
        val (morningCourses, afternoonCourses) = WidgetDataFetcher.getTodayCourses()

        val calendar = Calendar.getInstance()
        var currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (currentDayOfWeek == 0) currentDayOfWeek = 7

        // 6-block grouped layout to ensure NO TRUNCATION while preserving Morning/Afternoon/Evening labels
        val morningBlocks = listOf(listOf(1, 2), listOf(3, 4))
        val afternoonBlocks = listOf(listOf(5, 6), listOf(7, 8))
        
        val hasEvening = (1..7).any { day ->
            weeklyMap[day]?.any { (it.second.sectionName.toIntOrNull() ?: 0) > 8 } == true
        }
        val eveningBlocks = if (hasEvening) listOf(listOf(9, 10), listOf(11, 12)) else emptyList()

        provideContent {
            val size = LocalSize.current
            if (size.width < 250.dp) {
                TodayFallbackContent(context, morningCourses, afternoonCourses)
            } else {
                WeeklyScheduleContent(
                    context = context,
                    currentWeek = currentWeek,
                    currentDayOfWeek = currentDayOfWeek,
                    morningBlocks = morningBlocks,
                    afternoonBlocks = afternoonBlocks,
                    eveningBlocks = eveningBlocks,
                    weeklyMap = weeklyMap
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TodayFallbackContent(
        context: Context,
        morning: List<Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>>,
        afternoon: List<Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>>
    ) {
        val bgSurface = DayNightColorProvider(day = Color(0xFFF3F4F6), night = Color(0xFF1F2937))
        val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
        val textTertiary = DayNightColorProvider(day = Color.Gray, night = Color.Gray)

        val allCourses = morning + afternoon
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgSurface)
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            if (allCourses.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("今日无课", style = TextStyle(color = textTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    item {
                        Text("今日课程", style = TextStyle(color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp))
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                    items(allCourses) { coursePair ->
                        val (course, slot) = coursePair
                        val time = course.times.find {
                            val (start, _) = WidgetDataFetcher.parsePeriod(it.period)
                            start.toString() == slot.sectionName
                        }
                        val theme = getCourseTheme(course.course.courseName)
                        val resId = context.resources.getIdentifier("${theme.baseName}_single", "drawable", context.packageName)
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .let { if (resId != 0) it.background(ImageProvider(resId)) else it.background(ColorProvider(Color(0xFFE0E0E0))) }
                                .padding(8.dp)
                        ) {
                            Text(course.course.courseName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorProvider(Color(theme.textHex))), maxLines = 2)
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${slot.startTime} - ${slot.endTime}", style = TextStyle(color = ColorProvider(Color(theme.textHex)), fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(time?.location ?: "", style = TextStyle(color = ColorProvider(Color(theme.textHex)), fontSize = 10.sp), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WeeklyScheduleContent(
        context: Context,
        currentWeek: Int,
        currentDayOfWeek: Int,
        morningBlocks: List<List<Int>>,
        afternoonBlocks: List<List<Int>>,
        eveningBlocks: List<List<Int>>,
        weeklyMap: Map<Int, List<Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>>>
    ) {
        val bgSurface = DayNightColorProvider(day = Color(0xFFF3F4F6), night = Color(0xFF1F2937))
        val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
        val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)
        val textTertiary = DayNightColorProvider(day = Color.Gray, night = Color.Gray)
        val todayBgHeader = DayNightColorProvider(day = Color(0xFFE0F2FE), night = Color(0xFF1E3A8A).copy(alpha = 0.3f))
        val todayBgCol = DayNightColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF374151).copy(alpha = 0.3f))
        val highlightText = DayNightColorProvider(day = Color(0xFF007AFF), night = Color(0xFF60A5FA))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgSurface)
                .cornerRadius(16.dp)
                .padding(8.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = "第 ${currentWeek} 周",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                )

                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header Row
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        for (day in 1..7) {
                            val isToday = day == currentDayOfWeek
                            Box(
                                modifier = GlanceModifier.defaultWeight().let {
                                    if (isToday) it.background(todayBgHeader).cornerRadius(4.dp) else it
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNames[day - 1],
                                    style = TextStyle(
                                        color = if (isToday) highlightText else if (day >= 6) highlightText else textTertiary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    val allBlocks = morningBlocks + afternoonBlocks + eveningBlocks
                    if (allBlocks.isNotEmpty()) {
                        if (morningBlocks.isNotEmpty()) {
                            SectionLabelRow("上午", currentDayOfWeek)
                            morningBlocks.forEach { block ->
                                PeriodRow(
                                    context = context,
                                    blockPeriods = block,
                                    weeklyMap = weeklyMap,
                                    currentDayOfWeek = currentDayOfWeek,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }

                        if (afternoonBlocks.isNotEmpty()) {
                            SectionLabelRow("下午", currentDayOfWeek)
                            afternoonBlocks.forEach { block ->
                                PeriodRow(
                                    context = context,
                                    blockPeriods = block,
                                    weeklyMap = weeklyMap,
                                    currentDayOfWeek = currentDayOfWeek,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }

                        if (eveningBlocks.isNotEmpty()) {
                            SectionLabelRow("晚上", currentDayOfWeek)
                            eveningBlocks.forEach { block ->
                                PeriodRow(
                                    context = context,
                                    blockPeriods = block,
                                    weeklyMap = weeklyMap,
                                    currentDayOfWeek = currentDayOfWeek,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                    } else {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("本周无课", style = TextStyle(color = textTertiary, fontSize = 14.sp))
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SectionLabelRow(label: String, currentDayOfWeek: Int) {
        val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)
        val todayBgCol = DayNightColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF374151).copy(alpha = 0.3f))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            for (day in 1..7) {
                val isToday = day == currentDayOfWeek
                Box(
                    modifier = GlanceModifier.defaultWeight().let {
                        if (isToday) it.background(todayBgCol) else it
                    }.padding(horizontal = 1.dp)
                ) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth())
                }
            }
        }
    }

    private fun findCourseCoveringPeriod(
        weeklyMap: Map<Int, List<Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>>>,
        day: Int,
        period: Int
    ): Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>? {
        return weeklyMap[day]?.find { pair ->
            val time = pair.first.times.find { t ->
                val (s, _) = WidgetDataFetcher.parsePeriod(t.period)
                s.toString() == pair.second.sectionName
            }
            if (time != null) {
                val (start, span) = WidgetDataFetcher.parsePeriod(time.period)
                period in (start until (start + span))
            } else {
                false
            }
        }
    }

    private fun getCoursePart(course: CourseWithTimes, period: Int): String {
        val time = course.times.find { t ->
            val (s, span) = WidgetDataFetcher.parsePeriod(t.period)
            period in (s until (s + span))
        } ?: return "single"
        val (start, span) = WidgetDataFetcher.parsePeriod(time.period)
        return when {
            span == 1 -> "single"
            period == start -> "top"
            period == start + span - 1 -> "bottom"
            else -> "mid"
        }
    }

    @androidx.compose.runtime.Composable
    private fun PeriodRow(
        context: Context,
        blockPeriods: List<Int>,
        weeklyMap: Map<Int, List<Pair<CourseWithTimes, WidgetDataFetcher.TimeSlotConfig>>>,
        currentDayOfWeek: Int,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val textTertiary = DayNightColorProvider(day = Color.Gray, night = Color.Gray)
        val todayBgCol = DayNightColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF374151).copy(alpha = 0.3f))

        // Zero vertical padding for seamless continuity
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    blockPeriods.forEach { p ->
                        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = p.toString(),
                                style = TextStyle(
                                    color = textTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // 7 days columns
            for (day in 1..7) {
                val isToday = day == currentDayOfWeek
                
                val p1 = blockPeriods.getOrNull(0) ?: 0
                val p2 = blockPeriods.getOrNull(1) ?: 0

                val course1 = findCourseCoveringPeriod(weeklyMap, day, p1)
                val course2 = findCourseCoveringPeriod(weeklyMap, day, p2)

                Box(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight().let {
                        if (isToday) it.background(todayBgCol) else it
                    }.padding(horizontal = 1.dp), // Zero vertical padding
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = GlanceModifier.fillMaxSize()) {
                        if (course1 != null && course1 == course2) {
                            // Same course covers BOTH periods -> Full block
                            val theme = getCourseTheme(course1.first.course.courseName)
                            val part1 = getCoursePart(course1.first, p1)
                            val part2 = getCoursePart(course1.first, p2)
                            
                            val shape = if (part1 == "top" && part2 == "bottom") "single"
                                        else if (part1 == "top") "top"
                                        else if (part2 == "bottom") "bottom"
                                        else "mid"
                            
                            val resName = "${theme.baseName}_$shape"
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            
                            CourseBlock(
                                context = context,
                                courseName = course1.first.course.courseName,
                                theme = theme,
                                resId = resId,
                                isStart = (part1 == "top" || part1 == "single" || p1 == 5 || p1 == 9),
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        } else if (course1 != null || course2 != null) {
                            // Subdivided block
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                if (course1 != null) {
                                    val theme1 = getCourseTheme(course1.first.course.courseName)
                                    val part1 = getCoursePart(course1.first, p1)
                                    val resName1 = "${theme1.baseName}_$part1"
                                    val resId1 = context.resources.getIdentifier(resName1, "drawable", context.packageName)
                                    
                                    CourseBlock(
                                        context = context,
                                        courseName = course1.first.course.courseName,
                                        theme = theme1,
                                        resId = resId1,
                                        isStart = (part1 == "top" || part1 == "single" || p1 == 5 || p1 == 9),
                                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                                    )
                                } else {
                                    Spacer(modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                                }

                                if (course2 != null) {
                                    val theme2 = getCourseTheme(course2.first.course.courseName)
                                    val part2 = getCoursePart(course2.first, p2)
                                    val resName2 = "${theme2.baseName}_$part2"
                                    val resId2 = context.resources.getIdentifier(resName2, "drawable", context.packageName)
                                    
                                    CourseBlock(
                                        context = context,
                                        courseName = course2.first.course.courseName,
                                        theme = theme2,
                                        resId = resId2,
                                        isStart = (part2 == "top" || part2 == "single"),
                                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                                    )
                                } else {
                                    Spacer(modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                                }
                            }
                        } else {
                            // Empty
                            Spacer(modifier = GlanceModifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CourseBlock(
        context: Context,
        courseName: String,
        theme: CourseTheme,
        resId: Int,
        isStart: Boolean,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .let {
                    if (resId != 0) it.background(ImageProvider(resId))
                    else it.background(ColorProvider(Color(0xFFE0E0E0)))
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isStart) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = courseName,
                    style = TextStyle(
                        color = ColorProvider(Color(theme.textHex)),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 4
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else {
                Spacer(modifier = GlanceModifier.fillMaxSize())
            }
        }
    }
}
