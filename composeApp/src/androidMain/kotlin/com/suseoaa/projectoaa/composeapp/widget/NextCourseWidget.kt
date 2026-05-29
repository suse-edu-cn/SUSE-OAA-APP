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

class NextCourseWidget : GlanceAppWidget() {

    enum class CourseTheme(val bgHex: Long, val textHex: Long, val titleHex: Long) {
        BLUE(0xFFE0F2FE, 0xFF0284C7, 0xFF0C4A6E),
        GREEN(0xFFDCFCE7, 0xFF16A34A, 0xFF14532D),
        PINK(0xFFFCE7F3, 0xFFDB2777, 0xFF831843),
        ORANGE(0xFFFFEDD5, 0xFFEA580C, 0xFF7C2D12),
        PURPLE(0xFFF3E8FF, 0xFF9333EA, 0xFF581C87)
    }

    private fun getCourseTheme(name: String): CourseTheme {
        val themes = CourseTheme.values()
        val index = Math.abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nextCourseData = WidgetDataFetcher.getNextCourse()

        provideContent {
            if (nextCourseData == null) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.White))
                        .cornerRadius(12.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无课",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                val (course, slot) = nextCourseData
                val time = course.times.find { 
                    val (start, _) = WidgetDataFetcher.parsePeriod(it.period)
                    start.toString() == slot.sectionName
                }
                
                val theme = getCourseTheme(course.course.courseName)

                val bgSurface = DayNightColorProvider(day = Color.White, night = Color(0xFF1F2937))
                val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
                val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)
                val textTertiary = DayNightColorProvider(day = Color.Gray, night = Color.Gray)

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgSurface)
                        .cornerRadius(12.dp)
                        .padding(12.dp)
                ) {
                    val timeParts = slot.startTime.split(":")
                    val hour = timeParts.getOrNull(0) ?: "00"
                    val minute = timeParts.getOrNull(1) ?: "00"
                    
                    val badgeBg = DayNightColorProvider(day = Color(theme.bgHex), night = Color(theme.bgHex).copy(alpha = 0.2f))
                    val badgeTitle = DayNightColorProvider(day = Color(theme.titleHex), night = Color(theme.textHex))

                    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        // Left: Artistic Stacked Time Badge
                        Column(
                            modifier = GlanceModifier
                                .background(badgeBg)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hour,
                                style = TextStyle(
                                    color = badgeTitle,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = minute,
                                style = TextStyle(
                                    color = badgeTitle,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(16.dp))

                        // Right: Course Information Typography
                        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            
                            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "下一节课",
                                    style = TextStyle(
                                        color = DayNightColorProvider(day = Color(theme.textHex), night = Color(theme.bgHex)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = "至 ${slot.endTime}",
                                    style = TextStyle(
                                        color = textTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            
                            Text(
                                text = course.course.courseName,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textPrimary
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = GlanceModifier.height(2.dp))

                            Text(
                                text = "${time?.location ?: "未知地点"}  |  第${slot.sectionName}节",
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }
    }
}
