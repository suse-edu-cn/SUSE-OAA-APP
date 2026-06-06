@file:SuppressLint("RestrictedApi")
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
import androidx.glance.appwidget.cornerRadius
import kotlin.math.abs
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

import com.suseoaa.projectoaa.shared.data.repository.ExamCacheEntity
import com.suseoaa.projectoaa.shared.util.OaaClock
import com.suseoaa.projectoaa.shared.util.parseExamTimeRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class RecentExamsWidget : GlanceAppWidget() {

    enum class ExamTheme(val bgHex: Long, val textHex: Long, val titleHex: Long) {
        RED(0xFFFEE2E2, 0xFFDC2626, 0xFF7F1D1D),
        BLUE(0xFFE0F2FE, 0xFF0284C7, 0xFF0C4A6E),
        GREEN(0xFFDCFCE7, 0xFF16A34A, 0xFF14532D),
        ORANGE(0xFFFFEDD5, 0xFFEA580C, 0xFF7C2D12),
        PURPLE(0xFFF3E8FF, 0xFF9333EA, 0xFF581C87)
    }

    private fun getExamTheme(name: String): ExamTheme {
        val themes = ExamTheme.entries.toTypedArray()
        val index = abs(name.hashCode()) % themes.size
        return themes[index]
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var errorMsg: String? = null
        var summary: WidgetDataFetcher.ExamsSummary? = null
        try {
            summary = WidgetDataFetcher.getExamsSummary()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.stackTraceToString()
        }

        provideContent {
            val bgSurface = DayNightColorProvider(day = Color.White, night = Color(0xFF1F2937))
            val textPrimary = DayNightColorProvider(day = Color.Black, night = Color.White)
            val textSecondary = DayNightColorProvider(day = Color.DarkGray, night = Color.LightGray)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(12.dp)
                    .clickable(actionRunCallback<UpdateAndLaunchExamsAction>())
                    .padding(12.dp)
            ) {
                if (errorMsg != null) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "加载失败: ${errorMsg.take(50)}",
                            style = TextStyle(color = ColorProvider(Color.Red), fontSize = 10.sp)
                        )
                    }
                } else if (summary?.upcoming.isNullOrEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "近期无考试，好好休息！",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(
                                text = "近期考试",
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                text = "未考${summary!!.unTakenCount}门，已考${summary!!.takenCount}门",
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        val maxExams = summary!!.upcoming
                        maxExams.forEachIndexed { index, exam ->
                            val theme = getExamTheme(exam.courseName)
                            
                            @SuppressLint("RestrictedApi")
                            val badgeBg = DayNightColorProvider(day = Color(theme.bgHex), night = Color(theme.bgHex).copy(alpha = 0.2f))
                            
                            @SuppressLint("RestrictedApi")
                            val badgeTitle = DayNightColorProvider(day = Color(theme.titleHex), night = Color(theme.textHex))

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight()
                                    .clickable(actionRunCallback<UpdateAndLaunchExamsAction>())
                                    .padding(bottom = if (index < maxExams.size - 1) 4.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：徽章
                                    Column(
                                        modifier = GlanceModifier
                                            .background(badgeBg)
                                            .cornerRadius(6.dp)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                            .width(44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "考试",
                                            style = TextStyle(
                                                color = badgeTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = GlanceModifier.width(10.dp))

                                    // 中间：详情
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = exam.courseName,
                                            style = TextStyle(
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = GlanceModifier.height(4.dp))
                                        Text(
                                            text = "${exam.time} | ${exam.location}",
                                            style = TextStyle(
                                                color = textSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }

                                    // 右侧：倒计时
                                    val parsedTime = parseExamTimeRange(exam.time)
                                    val daysLeftStr = if (parsedTime != null) {
                                        val now = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                        val examDate = parsedTime.first.date
                                        val daysLeft = examDate.toEpochDays() - now.toEpochDays()
                                        if (daysLeft > 0) "剩${daysLeft}天"
                                        else if (daysLeft == 0) "今天"
                                        else ""
                                    } else ""

                                    if (daysLeftStr.isNotEmpty()) {
                                        Text(
                                            text = daysLeftStr,
                                            style = TextStyle(
                                                color = ColorProvider(Color(theme.titleHex)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = GlanceModifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

class UpdateAndLaunchExamsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("app://suseoaa/exams")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        RecentExamsWidget().updateAll(context)
    }
}
