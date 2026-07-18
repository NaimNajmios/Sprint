package com.najmi.sprint.core.ai.classifier

import com.najmi.sprint.core.ai.client.GroqClient
import com.najmi.sprint.core.domain.model.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.najmi.sprint.core.domain.logger.AppLogger

@Serializable
data class ActorClassificationResponse(
    val contextId: String,
    val confidence: Float
)

@Serializable
data class CriticReviewResponse(
    val approved: Boolean,
    val finalContextId: String?,
    val finalConfidence: Float?
)

class SessionClassifier @Inject constructor(
    private val groqClient: GroqClient,
    private val json: Json
) {
    private val TAG = "SessionClassifier"
    
    private fun extractJson(raw: String): String {
        return raw.substringAfter("```json").substringAfter("```").substringBeforeLast("```").trim()
    }
    /**
     * The Actor is responsible for taking a raw app package and predicting the correct context.
     */
    suspend fun actorClassify(
        packageName: String,
        appName: String?,
        category: String?,
        activeContexts: List<Context>
    ): ActorClassificationResponse? {
        val contextsJson = activeContexts.joinToString("\n") { "- ID: \${it.id}, Name: \${it.name}" }
        
        val systemPrompt = """
            You are an AI assistant categorizing Android app usage into contexts.
            Map the given Android app package name to the most appropriate context ID.
            If unknown, pick the most likely or default to 'Life'.
            
            Available Contexts:
            $contextsJson
            
            Respond strictly in JSON with "contextId" (string) and "confidence" (float 0.0 to 1.0).
        """.trimIndent()
        
        val appInfoStr = buildString {
            append("Package name: $packageName\n")
            if (appName != null) append("App Name: $appName\n")
            if (category != null && category != "Unknown") append("OS Category: $category\n")
        }
        
        val result = groqClient.generateCompletion(
            prompt = appInfoStr.trim(),
            systemPrompt = systemPrompt,
            jsonMode = true
        )
        AppLogger.d(TAG, "Actor Output for $packageName: $result")
        
        return try {
            json.decodeFromString<ActorClassificationResponse>(extractJson(result))
        } catch (e: Exception) {
            AppLogger.e(TAG, "Actor failed to parse JSON for $packageName", e)
            null
        }
    }

    /**
     * The Critic evaluates the Actor's prediction for plausibility and flags low-confidence answers.
     */
    suspend fun criticReview(
        packageName: String,
        appName: String?,
        category: String?,
        actorResponse: ActorClassificationResponse,
        activeContexts: List<Context>
    ): CriticReviewResponse? {
        val contextsJson = activeContexts.joinToString("\n") { "- ID: \${it.id}, Name: \${it.name}" }
        
        val systemPrompt = """
            You are a Critic AI reviewing an app classification.
            The Actor classified "$packageName" (App: ${appName ?: "Unknown"}, Category: ${category ?: "Unknown"}) as Context ID "${actorResponse.contextId}" with confidence ${actorResponse.confidence}.
            
            Available Contexts:
            $contextsJson
            
            Evaluate if this is plausible.
            Respond strictly in JSON with "approved" (boolean), "finalContextId" (string or null), and "finalConfidence" (float).
        """.trimIndent()

        val result = groqClient.generateCompletion(
            prompt = "Review the actor's classification.",
            systemPrompt = systemPrompt,
            model = "llama-3.1-8b-instant", // Use a smarter model for the critic
            jsonMode = true
        )
        AppLogger.d(TAG, "Critic Output for $packageName: $result")
        
        return try {
            json.decodeFromString<CriticReviewResponse>(extractJson(result))
        } catch (e: Exception) {
            AppLogger.e(TAG, "Critic failed to parse JSON for $packageName", e)
            null
        }
    }
}
