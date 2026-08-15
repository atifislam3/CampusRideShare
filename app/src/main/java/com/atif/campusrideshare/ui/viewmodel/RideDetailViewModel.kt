package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.LocationRepository
import com.atif.campusrideshare.data.repository.RequestRepository
import com.atif.campusrideshare.data.repository.RideRepository
import com.atif.campusrideshare.util.Config
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository,
    private val requestRepository: RequestRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val rideId: String = checkNotNull(savedStateHandle["rideId"])

    private val _uiState = MutableStateFlow<String?>(null) // Error messages
    val uiState: StateFlow<String?> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserModel?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ride: StateFlow<RideModel?> = rideRepository.observeRide(rideId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val requests: StateFlow<List<com.atif.campusrideshare.data.model.RideRequestModel>> = requestRepository.observeRequestsForRide(rideId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Driver: Location sharing job
    private var sharingJob: Job? = null
    private val _isLocationSharing = MutableStateFlow(false)
    val isLocationSharing: StateFlow<Boolean> = _isLocationSharing.asStateFlow()

    // Passenger: Observe driver location
    val driverLocation: StateFlow<Pair<Double, Double>?> = locationRepository.observeDriverLocation(rideId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleLocationSharing() {
        val currentRide = ride.value ?: return
        val user = currentUser.value ?: return
        
        if (currentRide.driverUid != user.uid) return

        if (_isLocationSharing.value) {
            sharingJob?.cancel()
            sharingJob = null
            _isLocationSharing.value = false
        } else {
            sharingJob = locationRepository.startSharingLocation(rideId, viewModelScope)
            _isLocationSharing.value = true
        }
    }

    fun requestToJoin() {
        val user = currentUser.value ?: return
        val currentRide = ride.value ?: return

        if (currentRide.driverUid == user.uid) {
            _uiState.value = "You cannot join your own ride"
            return
        }

        if (requests.value.any { it.passengerUid == user.uid }) {
            _uiState.value = "You have already requested to join this ride"
            return
        }

        viewModelScope.launch {
            requestRepository.sendRequest(
                rideId = rideId,
                passengerUid = user.uid,
                passengerName = user.fullName,
                passengerInitialsColor = user.initialsColor,
                passengerRating = user.averageRating
            ).onSuccess {
                _uiState.value = "SUCCESS: Request sent to driver!"
            }.onFailure { 
                _uiState.value = it.message 
            }
        }
    }

    fun clearError() {
        _uiState.value = null
    }

    fun markCompleted() {
        viewModelScope.launch {
            rideRepository.updateRideStatus(rideId, Config.STATUS_COMPLETED)
                .onFailure { _uiState.value = it.message }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharingJob?.cancel()
    }
}
