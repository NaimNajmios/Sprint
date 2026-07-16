package com.najmi.sprint.core.sync.auth

import com.najmi.sprint.core.sync.client.SupabaseClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AuthResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Int? = null,
    val refresh_token: String? = null,
    val user: SupabaseUser? = null
)

@Serializable
data class SupabaseUser(
    val id: String,
    val aud: String,
    val role: String,
    val email: String
)

@Singleton
class SupabaseAuthService @Inject constructor(
    private val client: SupabaseClient,
    private val authManager: AuthManager
) {
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            val response = client.authHttpClient.post("${client.supabaseUrl}/auth/v1/signup") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }
            if (response.status.isSuccess()) {
                val authData: AuthResponse = response.body()
                if (authData.access_token != null && authData.user != null) {
                    authManager.saveSession(authData.access_token, authData.user.id)
                    Result.success(Unit)
                } else {
                    // This happens when Supabase "Confirm Email" is enabled
                    Result.failure(Exception("Please check your email to verify your account before logging in."))
                }
            } else {
                Result.failure(Exception(response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = client.authHttpClient.post("${client.supabaseUrl}/auth/v1/token?grant_type=password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }
            if (response.status.isSuccess()) {
                val authData: AuthResponse = response.body()
                if (authData.access_token != null && authData.user != null) {
                    authManager.saveSession(authData.access_token, authData.user.id)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid login credentials"))
                }
            } else {
                Result.failure(Exception(response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        authManager.clearSession()
    }
}
