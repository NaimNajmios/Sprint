package com.najmi.sprint.core.ai.classifier

import com.najmi.sprint.core.ai.client.GroqClient
import com.najmi.sprint.core.domain.model.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ActorClassificationResponse(
    val contextId: String,
    val confidence: Float
)

@Serializable
data class CriticReviewResponse(
    val approved: Boolean,
    val finalContextId: String?,
    val finalConfidence: Float
)

class SessionClassifier @Inject constructor(
    private val groqClient: GroqClient,
    private val json: Json
) {
    /**
     * The Actor is responsible for taking a raw app package and predicting the correct context.
     */
    suspend fun actorClassify(
        packageName: String,
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
        
        val result = groqClient.generateCompletion(
            prompt = "Package name: $packageName",
            systemPrompt = systemPrompt,
            jsonMode = true
        )
        
        return try {
            json.decodeFromString<ActorClassificationResponse>(result)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The Critic evaluates the Actor's prediction for plausibility and flags low-confidence answers.
     */
    suspend fun criticReview(
        packageName: String,
        actorResponse: ActorClassificationResponse,
        activeContexts: List<Context>
    ): CriticReviewResponse? {
        val contextsJson = activeContexts.joinToString("\n") { "- ID: \${it.id}, Name: \${it.name}" }
        
        val systemPrompt = """
            You are a Critic AI reviewing an app classification.
            The Actor classified "$packageName" as Context ID "${actorResponse.contextId}" with confidence ${actorResponse.confidence}.
            
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
        
        return try {
            json.decodeFromString<CriticReviewResponse>(result)
        } catch (e: Exception) {
            null
        }
    }
}
