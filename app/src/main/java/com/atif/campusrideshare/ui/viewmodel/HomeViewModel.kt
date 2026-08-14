package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _vehicleFilter = MutableStateFlow<String?>(null) // null = all, "car", "bike"
    val vehicleFilter: StateFlow<String?> = _vehicleFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Combining raw ride stream with UI filter states
    val rides: StateFlow<List<RideModel>> = combine(
        rideRepository.getOpenRides(),
        _searchQuery,
        _vehicleFilter
    ) { allRides, query, filter ->
        allRides.filter { ride ->
            val matchesQuery = ride.destinationName.contains(query, ignoreCase = true) ||
                    ride.startAddress.contains(query, ignoreCase = true)
            val matchesFilter = filter == null || ride.vehicleType == filter
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onVehicleFilterChanged(filter: String?) {
        _vehicleFilter.value = filter
    }

    fun refresh() {
        // Since we are using a real-time listener (Flow) from Firebase, 
        // a "manual" refresh isn't strictly necessary for data fetching, 
        // but we trigger the UI loading state to provide user feedback.
        _isRefreshing.value = true
        kotlinx.coroutines.MainScope().run {
            _isRefreshing.value = false
        }
    }
}
