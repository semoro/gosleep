package me.semoro.gosleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import me.semoro.gosleep.data.UserSettings
import me.semoro.gosleep.ui.BedtimeZone
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@Composable
fun ZoneProgressIndicator(
    currentTime: Instant,
    userSettings: UserSettings,
    currentZone: BedtimeZone,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val greenColor = Color(0xFF4CAF50)
    val yellowColor = Color(0xFFFFC107)
    val redColor = Color(0xFFE91E63)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)  // Increased height to accommodate text
            .padding(horizontal = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val strokeWidth = height / 4

        val currentDateTime = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val bedtime = userSettings.calculateBedtimeStart(currentDateTime)
        val yellowStart = userSettings.calculateYellowZoneStart(currentDateTime)
        val redStart = userSettings.calculateRedZoneStart(currentDateTime)

        // Determine scale factor for both positioning and visual elements
        val wakeUpTime = userSettings.calculateWakeUpTime(currentDateTime)
        val minutesUntilWakeUp = ChronoUnit.MINUTES.between(currentTime.toJavaInstant(), wakeUpTime.toJavaInstant())

        // Determine scale based on time until wake-up
        val isSmallScale = minutesUntilWakeUp < 12 * 60
        val totalMinutes = if (isSmallScale) {
            // Less than 12 hours until wake-up, use 12-hour scale
            12 * 60f
        } else {
            // Otherwise use 24-hour scale
            24 * 60f
        }


        // Draw background line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Calculate positions
        fun Instant.toPosition(): Float {
            val thisMinutesUntilWakeUp = ChronoUnit.MINUTES.between(this.toJavaInstant(), wakeUpTime.toJavaInstant())

            // If time is after wake-up time, adjust to show it's for the next day
            val adjustedMinutes = if (thisMinutesUntilWakeUp < 0) {
                thisMinutesUntilWakeUp + 24 * 60 // Add 24 hours in minutes
            } else {
                thisMinutesUntilWakeUp
            }

            // Normalize position: 0 = wake-up time + totalMinutes, 1 = wake-up time
            val normalizedPosition = 1f - (adjustedMinutes / totalMinutes)
            return normalizedPosition * width
        }

        // Draw zones
        val bedtimePos = bedtime.toPosition()
        val yellowPos = yellowStart.toPosition()
        val redPos = redStart.toPosition()

        // Draw green zone
        drawLine(
            color = greenColor,
            start = Offset(bedtimePos, height / 2),
            end = Offset(yellowPos, height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw yellow zone
        drawLine(
            color = yellowColor,
            start = Offset(yellowPos, height / 2),
            end = Offset(redPos, height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw red zone
        drawLine(
            color = redColor,
            start = Offset(redPos, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw current time indicator
        val currentTimePos = currentTime.toPosition()
        drawCircle(
            color = when (currentZone) {
                BedtimeZone.GREEN -> greenColor
                BedtimeZone.YELLOW -> yellowColor
                BedtimeZone.RED -> redColor
                BedtimeZone.NONE -> indicatorColor
            },
            radius = (height / 5f) * 1f,
            center = Offset(currentTimePos, height / 2)
        )

        // Format times for display
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val wakeUpTimeFormatted = wakeUpTime.toLocalDateTime(TimeZone.currentSystemDefault()).time.toJavaLocalTime().format(timeFormatter)
        val bedtimeFormatted = bedtime.toLocalDateTime(TimeZone.currentSystemDefault()).time.toJavaLocalTime().format(timeFormatter)

        // Calculate time to sleep left
        val timeToSleepLeft = if (minutesUntilWakeUp > 0) {
            val hours = minutesUntilWakeUp / 60
            val mins = minutesUntilWakeUp % 60
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        } else {
            "0m"
        }

        // Draw text using drawText
        val textSize = 16.sp

        // Draw wakeup time on the right
        drawText(
            textMeasurer = textMeasurer,
            text = wakeUpTimeFormatted,
            topLeft = Offset(width - textMeasurer.measure(wakeUpTimeFormatted, TextStyle(fontSize = textSize)).size.width, height / 4 - textSize.toPx()),
            style = TextStyle(
                color = textColor,
                fontSize = textSize,
                textAlign = TextAlign.Right
            )
        )

        // Draw bedtime start under its position
        drawText(
            textMeasurer = textMeasurer,
            text = bedtimeFormatted,
            topLeft = Offset(
                bedtimePos - textMeasurer.measure(bedtimeFormatted, TextStyle(fontSize = textSize)).size.width / 2,
                height / 4 - textSize.toPx()
            ),
            style = TextStyle(
                color = textColor,
                fontSize = textSize,
                textAlign = TextAlign.Center
            )
        )

        // Draw time to sleep left over the progress indicator
        val timeToSleepText = timeToSleepLeft
        drawText(
            textMeasurer = textMeasurer,
            text = timeToSleepText,
            topLeft = Offset(currentTimePos, 0f) + Offset(
                - textMeasurer.measure(timeToSleepText, TextStyle(fontSize = textSize)).size.width / 2f,
                height / 2 + height / 4
            ),
            style = TextStyle(
                color = textColor,
                fontSize = textSize,
                textAlign = TextAlign.Center
            )
        )
    }
}
