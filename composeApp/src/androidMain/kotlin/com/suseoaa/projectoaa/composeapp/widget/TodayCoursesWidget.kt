package com.suseoaa.projectoaa.composeapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import androidx.glance.appwidget.cornerRadius

class TodayCoursesWidget : GlanceAppWidget() {

    enum class CourseTheme(val textHex: Long, val titleHex: Long) {
        BLUE(0xFF0284C7, 0xFF0C4A6E),
        GREEN(0xFF16A34A, 0xFF14532D),
        PINK(0xFFDB2777, 0xFF831843),
        ORANGE(0xFFEA580C, 0xFF7C2D12),
        PURPLE(0xFF9333EA, 0xFF581C87)
    }

    private fun getCourseTheme(name: String): CourseTheme {
        val themes = CourseTheme.values()
        val index = Math.abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val schedule = WidgetDataFetcher.getTodayCourses()

        provideContent {
            val bgSurface = DayNightColorProvider(day = Color.White, night = Color(0xFF1F2937))
            val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
            val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)
            val textTertiary = DayNightColorProvider(day = Color.Gray, night = Color.Gray)
            val dividerColor = DayNightColorProvider(day = Color(0xFFF3F4F6), night = Color(0xFF374151))
            val lightGrayIcon = DayNightColorProvider(day = Color.LightGray, night = Color.DarkGray)
            
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(16.dp)
                    .padding(16.dp)
            ) {
                val sections = listOf(
                    "上午" to schedule.morning,
                    "下午" to schedule.afternoon,
                    "晚上" to schedule.evening
                ).filter { it.second.isNotEmpty() }

                if (sections.isEmpty()) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "今日无课",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "好好休息一下吧！",
                            style = TextStyle(color = textTertiary, fontSize = 12.sp)
                        )
                    }
                } else {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        sections.forEachIndexed { index, (title, courses) ->
                            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                                // Header
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = GlanceModifier.width(3.dp).height(12.dp).background(lightGrayIcon).cornerRadius(1.5.dp)) {}
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                    Text(
                                        text = title,
                                        style = TextStyle(color = textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(10.dp))
                                
                                // Courses in this section
                                courses.forEach { (course, slot) ->
                                    CourseItem(course, slot, textPrimary, textSecondary)
                                    Spacer(modifier = GlanceModifier.height(10.dp))
                                }
                            }
                            
                            // Divider
                            if (index < sections.size - 1) {
                                Spacer(modifier = GlanceModifier.width(12.dp))
                                Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(dividerColor)) {}
                                Spacer(modifier = GlanceModifier.width(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CourseItem(
        course: CourseWithTimes, 
        slot: WidgetDataFetcher.TimeSlotConfig,
        textPrimary: ColorProvider,
        textSecondary: ColorProvider
    ) {
        val time = course.times.find { 
            val (start, _) = WidgetDataFetcher.parsePeriod(it.period)
            start.toString() == slot.sectionName
        }
        val theme = getCourseTheme(course.course.courseName)
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(DayNightColorProvider(day = Color(theme.textHex), night = Color(theme.textHex)))
                    .cornerRadius(1.5.dp)
            ) {}
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = course.course.courseName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.startTime,
                        style = TextStyle(color = ColorProvider(Color(theme.textHex)), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = time?.location ?: "未知",
                        style = TextStyle(color = textSecondary, fontSize = 11.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
