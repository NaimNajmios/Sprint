package com.najmi.sprint.tracking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.najmi.sprint.core.ai.classifier.SessionClassifier
import com.najmi.sprint.core.domain.model.ClassificationRule
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RuleRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import com.najmi.sprint.core.domain.logger.AppLogger

@HiltWorker
class ClassificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val contextRepository: ContextRepository,
    private val ruleRepository: RuleRepository,
    private val sessionClassifier: SessionClassifier
) : CoroutineWorker(appContext, workerParams) {

    private fun getAppMetadata(packageName: String): Pair<String, String>? {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            
            val categoryStr = when (appInfo.category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Game"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Video"
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Image"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Maps"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> "Unknown"
            }
            Pair(appName, categoryStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val activeContexts = contextRepository.observeActiveContexts().first()
            val unclassifiedSessions = sessionRepository.observeUnclassifiedSessions().first()

            if (unclassifiedSessions.isEmpty()) {
                return Result.success()
            }

            val prefs = applicationContext.getSharedPreferences("sprint_ai_prefs", Context.MODE_PRIVATE)
            val todayDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            val lastDate = prefs.getString("last_date", "")
            var callsToday = if (todayDate == lastDate) prefs.getInt("calls_today", 0) else 0
            val maxCallsPerDay = 50

            for (session in unclassifiedSessions) {
                if (callsToday >= maxCallsPerDay) {
                    AppLogger.w("ClassificationWorker", "Daily AI limit ($maxCallsPerDay) reached. Deferring to tomorrow.")
                    break
                }

                // Ignore currently active session
                if (session.endTime == null) continue

                // 1. Double check the Rule Table (in case a rule was added recently)
                val rule = ruleRepository.getRuleForPackage(session.rawLabel)
                if (rule != null) {
                    sessionRepository.updateSession(session.copy(contextId = rule.contextId))
                    continue
                }

                // 1.5. Stage 1: PackageManager Local Classification
                val metadata = getAppMetadata(session.rawLabel)
                val appName = metadata?.first
                val categoryStr = metadata?.second
                
                var localMatchId: String? = null
                if (metadata != null) {
                    for (ctx in activeContexts) {
                        val nameLower = ctx.name.lowercase()
                        val catLower = categoryStr!!.lowercase()
                        // E.g., if category is "Game" and context is "Gaming"
                        if (catLower != "unknown" && nameLower.contains(catLower)) {
                            localMatchId = ctx.id
                            break
                        }
                        // E.g., if app name is "WhatsApp" and context is "WhatsApp"
                        if (nameLower.contains(appName!!.lowercase())) {
                            localMatchId = ctx.id
                            break
                        }
                    }
                }
                
                if (localMatchId != null) {
                    AppLogger.d("ClassificationWorker", "Stage 1 Local Match: ${session.rawLabel} -> $localMatchId")
                    sessionRepository.updateSession(
                        session.copy(
                            contextId = localMatchId,
                            classificationConfidence = 1.0f // Local deterministic match
                        )
                    )
                    ruleRepository.insertOrUpdateRule(
                        ClassificationRule(
                            packageName = session.rawLabel,
                            contextId = localMatchId,
                            lastConfirmedAt = Clock.System.now()
                        )
                    )
                    continue
                }

                // 2. Stage 2: Escalate to Actor-Critic
                // Add delay to avoid aggressive burst rate limits
                delay(2000)
                val actorPrediction = sessionClassifier.actorClassify(
                    packageName = session.rawLabel, 
                    appName = appName,
                    category = categoryStr,
                    activeContexts = activeContexts
                ) ?: continue
                
                delay(1000)
                val criticValidation = sessionClassifier.criticReview(
                    packageName = session.rawLabel,
                    appName = appName,
                    category = categoryStr,
                    actorResponse = actorPrediction,
                    activeContexts = activeContexts
                ) ?: continue
                
                callsToday += 2
                prefs.edit()
                    .putString("last_date", todayDate)
                    .putInt("calls_today", callsToday)
                    .apply()

                // 3. Commit the result if approved
                val finalContextId = criticValidation.finalContextId
                if (criticValidation.approved && finalContextId != null) {
                    sessionRepository.updateSession(
                        session.copy(
                            contextId = finalContextId,
                            classificationConfidence = criticValidation.finalConfidence
                        )
                    )

                    // 4. Update Rule Table so we don't ask the AI for this app again!
                    ruleRepository.insertOrUpdateRule(
                        ClassificationRule(
                            packageName = session.rawLabel,
                            contextId = finalContextId,
                            lastConfirmedAt = Clock.System.now()
                        )
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            val isRateLimit = e.message?.contains("429") == true
            if (isRateLimit) {
                AppLogger.w("ClassificationWorker", "Groq Rate Limit (429) hit! Backing off.")
            } else {
                AppLogger.e("ClassificationWorker", "Failed to classify sessions", e)
            }
            Result.retry()
        }
    }
}
