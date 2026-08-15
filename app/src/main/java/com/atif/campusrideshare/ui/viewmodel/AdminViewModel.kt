package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.ReportModel
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.data.repository.AdminRepository
import com.atif.campusrideshare.data.repository.AuthRepository
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
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<String?>(null) // Error messages
    val uiState: StateFlow<String?> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    val reports: StateFlow<List<ReportModel>> = combineWithAuth { uid ->
        adminRepository.getAllReports(uid, _statusFilter.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserModel>> = combineWithAuth { uid ->
        adminRepository.getAllUsers(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveReport(reportId: String, adminNote: String, newStatus: String) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            adminRepository.resolveReport(uid, reportId, adminNote, newStatus)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun banUser(targetUid: String) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            adminRepository.banUser(uid, targetUid)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun unbanUser(targetUid: String) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            adminRepository.unbanUser(uid, targetUid)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
    }

    fun refresh() {
        _isRefreshing.value = true
        _isRefreshing.value = false
    }

    fun clearError() {
        _uiState.value = null
    }

    /**
     * Helper to flatMapLatest with the current user UID, ensuring defense-in-depth.
     */
    private fun <T> combineWithAuth(block: (String) -> kotlinx.coroutines.flow.Flow<T>): kotlinx.coroutines.flow.Flow<T> {
        return authRepository.observeCurrentUser().flatMapLatest { user ->
            if (user != null) block(user.uid)
            else flowOf(emptyList<Any>() as T)
        }
    }
}
