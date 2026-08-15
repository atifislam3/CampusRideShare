package com.atif.campusrideshare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.RideRepository
import com.atif.campusrideshare.service.OsrmRepository
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.VehicleUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

sealed class PostRideUiState {
    object Idle : PostRideUiState()
    object Loading : PostRideUiState()
    data class Success(val rideId: String) : PostRideUiState()
    data class Error(val message: String) : PostRideUiState()
}

@HiltViewModel
class PostRideViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository,
    private val osrmRepository: OsrmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostRideUiState>(PostRideUiState.Idle)
    val uiState: StateFlow<PostRideUiState> = _uiState.asStateFlow()

    private var currentUser: UserModel? = null

    // Form State
    var startLat by mutableStateOf(0.0)
    var startLng by mutableStateOf(0.0)
    var startAddress by mutableStateOf("")
    var destLat by mutableStateOf(0.0)
    var destLng by mutableStateOf(0.0)
    var destinationName by mutableStateOf("")
    var departureTime by mutableStateOf(0L)
    var note by mutableStateOf("")

    // Preview state
    var estimatedDistance by mutableStateOf(0.0)
    var estimatedCost by mutableStateOf(0.0)
    var routePolyline by mutableStateOf("")

    private val _vehicleType = MutableStateFlow(Config.VEHICLE_CAR)
    val vehicleType: StateFlow<String> = _vehicleType.asStateFlow()

    private val _seats = MutableStateFlow(Config.MAX_CAR_SEATS)
    val seats: StateFlow<Int> = _seats.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser = authRepository.getCurrentUser()
            currentUser?.let { user ->
                if (user.vehicleType.isNotEmpty()) {
                    setVehicleType(user.vehicleType)
                }
            }
        }
    }

    fun setVehicleType(type: String) {
        _vehicleType.value = type
        if (type == Config.VEHICLE_BIKE) {
            _seats.value = 1
        } else {
            // Default to max for car, or keep within range
            _seats.value = _seats.value.coerceIn(1, Config.MAX_CAR_SEATS)
        }
    }

    fun setSeats(count: Int) {
        if (_vehicleType.value == Config.VEHICLE_CAR) {
            _seats.value = count.coerceIn(1, Config.MAX_CAR_SEATS)
        } else {
            _seats.value = 1
        }
    }

    fun updateRoutePreview() {
        if (startLat != 0.0 && destLat != 0.0) {
            viewModelScope.launch {
                osrmRepository.getRouteDistanceAndPolyline(startLat, startLng, destLat, destLng)
                    .onSuccess { (distanceKm, polyline) ->
                        estimatedDistance = distanceKm
                        estimatedCost = round(distanceKm * Config.RS_PER_KM)
                        routePolyline = polyline
                    }
            }
        }
    }

    fun submitRide() {
        val user = currentUser ?: return
        
        viewModelScope.launch {
            _uiState.value = PostRideUiState.Loading
            
            // Re-fetch route if it changed or just use cached preview
            val distance = if (estimatedDistance > 0) estimatedDistance else 0.0
            val polyline = if (routePolyline.isNotEmpty()) routePolyline else ""
            val cost = if (estimatedCost > 0) estimatedCost else 0.0

            if (distance == 0.0) {
                // Fetch if not available
                val routeResult = osrmRepository.getRouteDistanceAndPolyline(startLat, startLng, destLat, destLng)
                routeResult.onSuccess { (d, p) ->
                    createRide(user, d, p, round(d * Config.RS_PER_KM))
                }.onFailure {
                    _uiState.value = PostRideUiState.Error("Routing error: ${it.message}")
                }
            } else {
                createRide(user, distance, polyline, cost)
            }
        }
    }

    private suspend fun createRide(user: UserModel, distanceKm: Double, polyline: String, cost: Double) {
        val ride = RideModel(
            driverUid = user.uid,
            driverName = user.fullName,
            driverInitialsColor = user.initialsColor,
            driverRating = user.averageRating,
            vehicleType = _vehicleType.value,
            vehicleModel = user.vehicleModel,
            vehicleColor = user.vehicleColor,
            vehiclePlate = user.vehiclePlate,
            startLat = startLat,
            startLng = startLng,
            startAddress = startAddress,
            destinationName = destinationName,
            destLat = destLat,
            destLng = destLng,
            distanceKm = distanceKm,
            costPerSeat = cost,
            totalSeats = _seats.value,
            seatsLeft = _seats.value,
            departureTime = departureTime,
            note = note,
            routePolyline = polyline
        )

        rideRepository.postRide(ride)
            .onSuccess { _uiState.value = PostRideUiState.Success(it) }
            .onFailure { _uiState.value = PostRideUiState.Error(it.message ?: "Failed to post ride") }
    }

    fun clearState() {
        _uiState.value = PostRideUiState.Idle
    }
}
