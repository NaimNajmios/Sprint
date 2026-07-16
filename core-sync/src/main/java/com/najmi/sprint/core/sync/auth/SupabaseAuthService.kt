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
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val user: SupabaseUser
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
            val response = client.authHttpClient.post("\${client.supabaseUrl}/auth/v1/signup") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }
            if (response.status.isSuccess()) {
                val authData: AuthResponse = response.body()
                authManager.saveSession(authData.access_token, authData.user.id)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = client.authHttpClient.post("\${client.supabaseUrl}/auth/v1/token?grant_type=password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }
            if (response.status.isSuccess()) {
                val authData: AuthResponse = response.body()
                authManager.saveSession(authData.access_token, authData.user.id)
                Result.success(Unit)
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
