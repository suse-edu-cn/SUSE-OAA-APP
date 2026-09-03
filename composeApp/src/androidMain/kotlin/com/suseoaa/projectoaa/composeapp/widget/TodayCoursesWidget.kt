@file:SuppressLint("RestrictedApi")
package com.suseoaa.projectoaa.composeapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.ContentScale
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.appwidget.cornerRadius
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import kotlin.math.abs
class TodayCoursesWidget : GlanceAppWidget() {

    enum class CourseTheme(val bgHex: Long, val textHex: Long, val titleHex: Long) {
        BLUE(0xFFE0F2FE, 0xFF0284C7, 0xFF0C4A6E),
        GREEN(0xFFDCFCE7, 0xFF16A34A, 0xFF14532D),
        PINK(0xFFFCE7F3, 0xFFDB2777, 0xFF831843),
        ORANGE(0xFFFFEDD5, 0xFFEA580C, 0xFF7C2D12),
        PURPLE(0xFFF3E8FF, 0xFF9333EA, 0xFF581C87)
    }

    private fun getCourseTheme(name: String): CourseTheme {
        val themes = CourseTheme.entries.toTypedArray()
        val index = abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var errorMsg: String? = null
        var todaySchedule: WidgetDataFetcher.TodaySchedule? = null
        try {
            todaySchedule = WidgetDataFetcher.getTodayCourses()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.stackTraceToString()
        }

        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val month = now.monthNumber
        val day = now.dayOfMonth
        val weekdayStr = when(now.dayOfWeek.ordinal + 1) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            7 -> "日"
            else -> ""
        }

        provideContent {
            val bgSurface = DayNightColorProvider(day = Color.White, night = Color(0xFF1F2937))
            val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
            val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("app://suseoaa/main?tab=1")).apply {
                setPackage(context.packageName)
            }

            if (errorMsg != null) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgSurface)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(intent))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载失败: ${errorMsg.take(50)}",
                        style = TextStyle(color = ColorProvider(Color.Red), fontSize = 10.sp)
                    )
                }
            } else if (todaySchedule == null || (todaySchedule.morning.isEmpty() && todaySchedule.afternoon.isEmpty() && todaySchedule.evening.isEmpty())) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgSurface)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(intent))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今日无课，好好休息！",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgSurface)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(intent))
                        .padding(12.dp)
                ) {
                    val allCourses = todaySchedule.morning + todaySchedule.afternoon + todaySchedule.evening
                    
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "今日周$weekdayStr，共有 ",
                                        style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${allCourses.size}",
                                        style = TextStyle(color = ColorProvider(Color(0xFFF472B6)), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = " 门课要上",
                                        style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "$month.$day / 今日课程",
                                    style = TextStyle(color = textSecondary, fontSize = 10.sp)
                                )
                            }
                            // 青蟹的logo
                            @SuppressLint("DiscouragedApi")
                            val logoResId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
                            Image(
                                provider = ImageProvider(logoResId),
                                contentDescription = "Logo",
                                modifier = GlanceModifier.width(36.dp).height(36.dp).cornerRadius(8.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        
                        val chunks = allCourses.chunked(2)
                        chunks.forEach { rowCourses ->
                            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                rowCourses.forEachIndexed { index, (course, slot) ->
                                    val isFirst = index == 0
                                    Box(
                                        modifier = GlanceModifier.defaultWeight()
                                            .padding(end = if (isFirst && rowCourses.size == 2) 8.dp else 0.dp)
                                    ) {
                                        @SuppressLint("RestrictedApi")
                                        CourseItem(course, slot)
                                    }
                                }
                                if (rowCourses.size == 1) {
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi", "ComposableNaming")
    @androidx.compose.runtime.Composable
    private fun CourseItem(
        course: CourseWithTimes, 
        slot: WidgetDataFetcher.TimeSlotConfig
    ) {
        val time = course.times.find { 
            val (start, _) = WidgetDataFetcher.parsePeriod(it.period)
            start.toString() == slot.sectionName
        }
        val theme = getCourseTheme(course.course.courseName)
        val bgProvider = DayNightColorProvider(day = Color(theme.bgHex), night = Color(theme.bgHex).copy(alpha = 0.2f))
        val textProvider = DayNightColorProvider(day = Color(theme.titleHex), night = Color(theme.textHex))
        
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(bgProvider)
                .cornerRadius(12.dp)
                .padding(10.dp)
        ) {
            Text(
                text = course.course.courseName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = textProvider,
                    fontSize = 12.sp
                ),
                maxLines = 2
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "${slot.startTime} - ${slot.endTime}  ${time?.location ?: ""}",
                style = TextStyle(
                    color = textProvider,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}
