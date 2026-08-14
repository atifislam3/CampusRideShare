package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.RequestRepository
import com.atif.campusrideshare.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyRidesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val ridesAsDriver: StateFlow<List<RideModel>> = authRepository.observeCurrentUser()
        .flatMapLatest { user ->
            if (user != null) rideRepository.getMyRidesAsDriver(user.uid)
            else flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ridesAsPassenger: StateFlow<List<RideModel>> = authRepository.observeCurrentUser()
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else requestRepository.observeMyRequests(user.uid).flatMapLatest { requests ->
                if (requests.isEmpty()) flowOf(emptyList())
                else {
                    // Observe each ride found in the requests
                    val rideFlows = requests.map { req -> 
                        rideRepository.observeRide(req.rideId) 
                    }
                    combine(rideFlows) { it.filterNotNull() }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refresh() {
        _isRefreshing.value = true
        // In a real scenario, this might trigger a manual fetch if using a Cache/DB pattern.
        // For Firebase Flow, we just pulse the UI for feedback.
        kotlinx.coroutines.MainScope().run {
            _isRefreshing.value = false
        }
    }
}
