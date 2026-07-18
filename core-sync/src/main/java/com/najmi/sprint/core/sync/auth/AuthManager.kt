package com.najmi.sprint.core.sync.auth

import android.content.Context
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
    
    private val _refreshToken = MutableStateFlow<String?>(prefs.getString("refresh_token", null))
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()
    
    private val _userId = MutableStateFlow<String?>(prefs.getString("user_id", null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    fun saveSession(token: String, refresh: String?, uid: String) {
        prefs.edit().apply {
            putString("access_token", token)
            if (refresh != null) putString("refresh_token", refresh)
            putString("user_id", uid)
            apply()
        }
        _accessToken.value = token
        _refreshToken.value = refresh
        _userId.value = uid
    }

    fun clearSession() {
        prefs.edit().apply {
            remove("access_token")
            remove("refresh_token")
            remove("user_id")
            apply()
        }
        _accessToken.value = null
        _refreshToken.value = null
        _userId.value = null
    }

    fun getAccessToken(): String? = _accessToken.value
    fun getRefreshToken(): String? = _refreshToken.value
    fun getUserId(): String? = _userId.value
    fun isLoggedIn(): Boolean = _accessToken.value != null
}
