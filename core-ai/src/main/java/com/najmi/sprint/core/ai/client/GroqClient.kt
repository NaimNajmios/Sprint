package com.najmi.sprint.core.ai.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import com.najmi.sprint.core.domain.logger.AppLogger

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Float = 0.0f,
    val response_format: GroqResponseFormat? = null
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponseFormat(
    val type: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)

class GroqClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String,
        model: String = "llama-3.1-8b-instant",
        jsonMode: Boolean = false
    ): String {
        val request = GroqRequest(
            model = model,
            messages = listOf(
                GroqMessage("system", systemPrompt),
                GroqMessage("user", prompt)
            ),
            response_format = if (jsonMode) GroqResponseFormat("json_object") else null
        )

        val response = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            AppLogger.e("GroqClient", "API Error: ${response.status} - $errorText")
            throw IllegalStateException("Groq API error: ${response.status.value}")
        }

        val body: GroqResponse = response.body()
        return body.choices.first().message.content
    }
}
