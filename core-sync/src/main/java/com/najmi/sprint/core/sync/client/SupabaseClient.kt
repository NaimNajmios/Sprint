package com.najmi.sprint.core.sync.client

import com.najmi.sprint.core.sync.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import com.najmi.sprint.core.domain.logger.AppLogger
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.najmi.sprint.core.sync.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val authManager: AuthManager
) {

    private val rawUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    val supabaseUrl = if (rawUrl.endsWith("/rest/v1")) rawUrl.removeSuffix("/rest/v1") else rawUrl
    val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    AppLogger.d("SupabaseClient", message)
                }
            }
            level = LogLevel.INFO
        }

        defaultRequest {
            url("$supabaseUrl/rest/v1/")
            contentType(ContentType.Application.Json)
            header("apikey", supabaseKey)
            
            val token = authManager.getAccessToken() ?: supabaseKey
            header(HttpHeaders.Authorization, "Bearer $token")
            // Prefer single object return instead of array when returning a single inserted row
            header("Prefer", "return=representation")
        }
    }

    val authHttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    AppLogger.d("SupabaseAuth", message)
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            header("apikey", supabaseKey)
        }
    }
}
