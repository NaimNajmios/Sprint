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
import com.najmi.sprint.core.sync.auth.AuthResponse
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.isSuccess
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

        install(Auth) {
            bearer {
                loadTokens {
                    val access = authManager.getAccessToken()
                    val refresh = authManager.getRefreshToken()
                    BearerTokens(access ?: supabaseKey, refresh ?: "")
                }
                refreshTokens {
                    val refresh = authManager.getRefreshToken() ?: return@refreshTokens null
                    try {
                        val response = authHttpClient.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refresh_token" to refresh))
                        }
                        if (response.status.isSuccess()) {
                            val authData: AuthResponse = response.body()
                            if (authData.access_token != null && authData.user != null) {
                                authManager.saveSession(authData.access_token, authData.refresh_token, authData.user.id)
                                BearerTokens(authData.access_token, authData.refresh_token ?: "")
                            } else null
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                sendWithoutRequest { true }
            }
        }

        defaultRequest {
            url("$supabaseUrl/rest/v1/")
            contentType(ContentType.Application.Json)
            header("apikey", supabaseKey)
            
            // Note: Authorization header is automatically added by Auth plugin.
            // But we need to add it manually for endpoints that don't trigger auth or for the first request if Auth isn't fully configured
            // But actually Auth plugin handles it.
            // Wait, Supabase requires Authorization header for all requests anyway.
            // Ktor Auth plugin sends Authorization header automatically if BearerTokens are loaded.
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
