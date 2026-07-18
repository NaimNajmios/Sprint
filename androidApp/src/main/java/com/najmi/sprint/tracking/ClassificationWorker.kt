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

    override suspend fun doWork(): Result {
        return try {
            val activeContexts = contextRepository.observeActiveContexts().first()
            val unclassifiedSessions = sessionRepository.observeUnclassifiedSessions().first()

            if (unclassifiedSessions.isEmpty()) {
                return Result.success()
            }

            for (session in unclassifiedSessions) {
                // Ignore currently active session
                if (session.endTime == null) continue

                // 1. Double check the Rule Table (in case a rule was added recently)
                val rule = ruleRepository.getRuleForPackage(session.rawLabel)
                if (rule != null) {
                    sessionRepository.updateSession(session.copy(contextId = rule.contextId))
                    continue
                }

                // 2. Escalate to Actor-Critic
                val actorPrediction = sessionClassifier.actorClassify(session.rawLabel, activeContexts) ?: continue
                
                val criticValidation = sessionClassifier.criticReview(
                    packageName = session.rawLabel,
                    actorResponse = actorPrediction,
                    activeContexts = activeContexts
                ) ?: continue

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
            AppLogger.e("ClassificationWorker", "Failed to classify sessions", e)
            Result.retry()
        }
    }
}
