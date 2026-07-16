package com.najmi.sprint.tracking

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.najmi.sprint.core.ai.client.GroqClient
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@HiltWorker
class RetroGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val contextRepository: ContextRepository,
    private val retroRepository: RetroRepository,
    private val groqClient: GroqClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RetroGenerationWorker"
        private const val PROMPT_VERSION = "v1.0"
        private const val MODEL = "llama-3.1-8b-instant"
    }

    override suspend fun doWork(): Result {
        return try {
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(tz).date
            val weekStart = today.minus(DatePeriod(days = 6))

            // Check if we already generated a retro for this week
            val existing = retroRepository.getRetroForWeek(weekStart)
            if (existing != null) {
                Log.d(TAG, "Retro already exists for week of $weekStart, skipping.")
                return Result.success()
            }

            val startInstant = weekStart.atStartOfDayIn(tz)
            val endInstant = today.minus(DatePeriod(days = -1)).atStartOfDayIn(tz)

            val sessions = sessionRepository.getSessionsBetween(startInstant, endInstant)
            val contexts = contextRepository.observeActiveContexts().first()

            if (sessions.isEmpty()) {
                Log.d(TAG, "No sessions found for this week, skipping retro generation.")
                return Result.success()
            }

            // Aggregate stats for the prompt
            val perContext = mutableMapOf<String, Long>()
            val perApp = mutableMapOf<String, Long>()
            var totalMinutes = 0L

            for (session in sessions) {
                val end = session.endTime ?: continue
                val duration = (end.minus(session.startTime)).inWholeMinutes
                totalMinutes += duration

                val ctxId = session.contextId ?: "unclassified"
                perContext[ctxId] = perContext.getOrDefault(ctxId, 0L) + duration
                perApp[session.rawLabel] = perApp.getOrDefault(session.rawLabel, 0L) + duration
            }

            // Build context name mapping
            val contextNames = perContext.entries.joinToString("\n") { (id, mins) ->
                val name = contexts.find { it.id == id }?.name ?: "Unclassified"
                "- $name: ${mins / 60}h ${mins % 60}m"
            }

            val topApps = perApp.entries.sortedByDescending { it.value }.take(5).joinToString("\n") { (pkg, mins) ->
                "- $pkg: ${mins / 60}h ${mins % 60}m"
            }

            val systemPrompt = """
                You are a productivity coach analyzing a user's weekly app usage data.
                Write a concise, encouraging 3-4 sentence weekly retrospective summary.
                Highlight the dominant context, mention the top apps, and give one actionable suggestion.
                Do NOT use markdown. Write plain text only.
            """.trimIndent()

            val userPrompt = """
                Week: $weekStart to $today
                Total tracked time: ${totalMinutes / 60}h ${totalMinutes % 60}m
                
                Time per context:
                $contextNames
                
                Top 5 apps:
                $topApps
            """.trimIndent()

            val summaryText = groqClient.generateCompletion(
                prompt = userPrompt,
                systemPrompt = systemPrompt,
                model = MODEL,
                jsonMode = false
            )

            // Determine the flagged context (the one with the least time, as it may need attention)
            val flaggedContextId = perContext.filter { it.key != "unclassified" }
                .minByOrNull { it.value }?.key

            val retroEntry = RetroEntry(
                id = UUID.randomUUID().toString(),
                weekOf = weekStart,
                summaryText = summaryText.trim(),
                flaggedContextId = flaggedContextId,
                generatedByModel = MODEL,
                promptVersion = PROMPT_VERSION,
                criticApproved = true // Single-pass for retros; no critic needed
            )

            retroRepository.insertRetro(retroEntry)
            Log.d(TAG, "Retro generated successfully for week of $weekStart")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate retro", e)
            Result.retry()
        }
    }
}
