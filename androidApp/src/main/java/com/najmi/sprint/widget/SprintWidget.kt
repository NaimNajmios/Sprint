package com.najmi.sprint.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import android.content.Intent
import android.content.ComponentName
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.core.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SprintWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SprintWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun sessionRepository(): SessionRepository
}

class SprintWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        
        val taskRepository = entryPoint.taskRepository()
        val sessionRepository = entryPoint.sessionRepository()

        // Fetch Top Task
        val topTask = taskRepository.observeTasksByStatus(TaskStatus.IN_PROGRESS)
            .firstOrNull()?.firstOrNull() ?: taskRepository.observeTasksByStatus(TaskStatus.BACKLOG).firstOrNull()?.firstOrNull()
            
        // Fetch Today's Sessions to calculate time
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val sessions = sessionRepository.observeSessionsForDate(today).firstOrNull() ?: emptyList()
        val totalMillis = sessions.sumOf { (it.endTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()) - it.startTime.toEpochMilliseconds() }
        val hours = totalMillis / (1000 * 60 * 60)
        val minutes = (totalMillis / (1000 * 60)) % 60
        
        val timeString = if (hours > 0) "${hours}h ${minutes}m Logged" else "${minutes}m Logged"

        provideContent {
            GlanceTheme {
                WidgetContent(timeString, topTask)
            }
        }
    }
}


val NavyHero = ColorProvider(Color(0xFF161A2C))
val Chartreuse = ColorProvider(Color(0xFFC8FF00))
val WhiteSheet = ColorProvider(Color(0xFFFFFFFF))
val DarkText = ColorProvider(Color(0xFF1C1D21))
val GrayText = ColorProvider(Color(0xFF737784))
val WhiteFaded = ColorProvider(Color.White.copy(alpha = 0.7f))

@Composable
fun WidgetContent(timeString: String, topTask: Task?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(NavyHero)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent().apply {
                component = ComponentName("com.najmi.sprint", "com.najmi.sprint.MainActivity")
            }))
    ) {
        // Daily Ledger: Hero metrics at the top
        Text(
            text = "TODAY's TRACKING",
            style = TextStyle(
                color = WhiteFaded,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        Text(
            text = timeString,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            modifier = GlanceModifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        // Top Task inside a white "Sheet" lookalike
        if (topTask != null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    // Glance doesn't support advanced rounding easily without XML drawables,
                    // but we can use cornerRadius (requires Android 12+) or just background.
                    .background(WhiteSheet)
                    .padding(12.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topTask.title,
                        style = TextStyle(
                            color = DarkText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = topTask.status.name.replace("_", " "),
                        style = TextStyle(
                            color = Chartreuse, // Just to add the brand splash
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        } else {
            Text(
                text = "No pending tasks!",
                style = TextStyle(
                    color = WhiteFaded,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
        }
    }
}
