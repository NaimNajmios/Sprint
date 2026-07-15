package com.najmi.sprint.core.ai.di

import com.najmi.sprint.core.ai.classifier.SessionClassifier
import com.najmi.sprint.core.ai.client.GroqClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    @Provides
    @Singleton
    fun provideGroqClient(httpClient: HttpClient): GroqClient {
        val apiKey = com.najmi.sprint.core.ai.BuildConfig.GROQ_API_KEY
        return GroqClient(httpClient, apiKey)
    }

    @Provides
    @Singleton
    fun provideSessionClassifier(groqClient: GroqClient, json: Json): SessionClassifier {
        return SessionClassifier(groqClient, json)
    }
}
