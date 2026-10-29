package com.example.simplecalendarwidget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

val OFFSET_KEY = intPreferencesKey("month_offset")

class CalendarWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val monthOffset = prefs[OFFSET_KEY] ?: 0
            val size = LocalSize.current

            val today = LocalDate.now()
            val targetDate = today.plusMonths(monthOffset.toLong())
            val targetYearMonth = YearMonth.from(targetDate)
            val firstOfMonth = targetYearMonth.atDay(1)

            val locale = Locale.getDefault()
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val daysBefore = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val gridStartDate = firstOfMonth.minusDays(daysBefore.toLong())

            val cellWidth = size.width / 7f
            val headerHeight = maxOf(48.dp, size.height * 0.12f)
            val weekdayHeight = size.height * 0.10f
            val rowHeight = (size.height - headerHeight - weekdayHeight) / 6f

            val scale = minOf(cellWidth.value, rowHeight.value)
            val textSize = (scale * 0.42f).sp
            val headerTextSize = (scale * 0.48f).sp
            val navTextSize = (scale * 0.65f).sp
            val circleSize = (scale * 0.82f).dp

            val white = ColorProvider(day = Color.White, night = Color.White)
            val gray = ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA))
            val dimGray = ColorProvider(day = Color(0xFF555555), night = Color(0xFF555555))

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(headerHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.width(maxOf(cellWidth * 1.2f, 48.dp)).fillMaxHeight()
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(ChangeMonthAction.DELTA_KEY to -1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            style = TextStyle(color = white, fontSize = navTextSize, textAlign = TextAlign.Center)
                        )
                    }

                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(ChangeMonthAction.DELTA_KEY to 0))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                            style = TextStyle(color = white, fontSize = headerTextSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                    }

                    Box(
                        modifier = GlanceModifier.width(maxOf(cellWidth * 1.2f, 48.dp)).fillMaxHeight()
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(ChangeMonthAction.DELTA_KEY to 1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            style = TextStyle(color = white, fontSize = navTextSize, textAlign = TextAlign.Center)
                        )
                    }
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(weekdayHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..6) {
                        val dow = firstDayOfWeek.plus(i.toLong())
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dow.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                                style = TextStyle(color = gray, fontSize = textSize, textAlign = TextAlign.Center)
                            )
                        }
                    }
                }

                for (r in 0..5) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0..6) {
                            val date = gridStartDate.plusDays((r * 7 + c).toLong())
                            val isToday = date == today
                            val inMonth = date.month == targetYearMonth.month && date.year == targetYearMonth.year

                            val textColor = when {
                                isToday -> white
                                inMonth -> white
                                else -> dimGray
                            }

                            Box(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = GlanceModifier.size(circleSize).background(ImageProvider(R.drawable.bg_circle)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = TextStyle(color = textColor, fontSize = textSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = TextStyle(color = textColor, fontSize = textSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
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

class ChangeMonthAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DELTA_KEY] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[OFFSET_KEY] ?: 0
            prefs[OFFSET_KEY] = if (delta == 0) 0 else current + delta
        }
        CalendarWidget().update(context, glanceId)
    }

    companion object {
        val DELTA_KEY = ActionParameters.Key<Int>("delta")
    }
}
