package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.NotificationModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<String?>(null) // Error messages
    val uiState: StateFlow<String?> = _uiState.asStateFlow()

    val notifications: StateFlow<List<NotificationModel>> = authRepository.observeCurrentUser()
        .flatMapLatest { user ->
            if (user != null) notificationRepository.observeMyNotifications(user.uid)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = authRepository.observeCurrentUser()
        .flatMapLatest { user ->
            if (user != null) notificationRepository.unreadCount(user.uid)
            else flowOf(0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markRead(notifId: String) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            notificationRepository.markAsRead(uid, notifId)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            notificationRepository.markAllAsRead(uid)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun clearError() {
        _uiState.value = null
    }
}
