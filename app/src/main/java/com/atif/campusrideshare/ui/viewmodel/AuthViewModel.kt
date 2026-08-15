package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Observes the current user from the repository and converts to StateFlow
    // Collection automatically stops when viewModelScope is cleared.
    val currentUser: StateFlow<UserModel?> = authRepository.observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign in failed") }
        }
    }

    fun signUp(fullName: String, email: String, password: String, phone: String, university: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                kotlinx.coroutines.withTimeout(30000) { // Increased to 30 second timeout
                    authRepository.signUp(fullName, email, password, phone, university)
                        .onSuccess { _uiState.value = AuthUiState.Success }
                        .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AuthUiState.Error("Sign up timed out. Please check your internet connection.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Google sign in failed") }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            authRepository.updateFcmToken(token)
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}
