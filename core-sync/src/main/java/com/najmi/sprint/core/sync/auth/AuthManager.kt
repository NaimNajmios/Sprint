package com.najmi.sprint.core.sync.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("sprint_auth_prefs", Context.MODE_PRIVATE)

    private val _accessToken = MutableStateFlow<String?>(prefs.getString("access_token", null))
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()
    
    private val _userId = MutableStateFlow<String?>(prefs.getString("user_id", null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    fun saveSession(token: String, uid: String) {
        prefs.edit(commit = true) {
            putString("access_token", token)
            putString("user_id", uid)
        }
        _accessToken.value = token
        _userId.value = uid
    }

    fun clearSession() {
        prefs.edit(commit = true) {
            remove("access_token")
            remove("user_id")
        }
        _accessToken.value = null
        _userId.value = null
    }

    fun getAccessToken(): String? = _accessToken.value
    fun getUserId(): String? = _userId.value
    fun isLoggedIn(): Boolean = _accessToken.value != null
}
