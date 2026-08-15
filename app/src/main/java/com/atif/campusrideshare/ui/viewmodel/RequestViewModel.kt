package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.RideRequestModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<String?>(null)
    val uiState: StateFlow<String?> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Mode: Driver viewing requests for a specific ride
    private val rideId: String? = savedStateHandle["rideId"]
    val incomingRequests: StateFlow<List<RideRequestModel>> = if (rideId != null) {
        requestRepository.observeRequestsForRide(rideId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    // Mode: Passenger viewing their own requests
    val myRequests: StateFlow<List<RideRequestModel>> = authRepository.observeCurrentUser()
        .flatMapLatest { user ->
            if (user != null) requestRepository.observeMyRequests(user.uid)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun respondToRequest(rideId: String, requestId: String, accept: Boolean) {
        viewModelScope.launch {
            requestRepository.respondToRequest(rideId, requestId, accept)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun cancelRequest(rideId: String, requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelMyRequest(rideId, requestId)
                .onFailure { _uiState.value = it.message }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        _isRefreshing.value = false
    }

    fun clearError() {
        _uiState.value = null
    }
}
