package com.najmi.sprint.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.sync.auth.SupabaseAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: SupabaseAuthService
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAuthSuccessful = MutableStateFlow(false)
    val isAuthSuccessful: StateFlow<Boolean> = _isAuthSuccessful.asStateFlow()

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Email and Password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = authService.login(_email.value.trim(), _password.value.trim())
            
            if (result.isSuccess) {
                _isAuthSuccessful.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
            _isLoading.value = false
        }
    }

    fun signUp() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Email and Password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authService.signUp(_email.value.trim(), _password.value.trim())

            if (result.isSuccess) {
                _isAuthSuccessful.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Sign Up failed"
            }
            _isLoading.value = false
        }
    }
}
