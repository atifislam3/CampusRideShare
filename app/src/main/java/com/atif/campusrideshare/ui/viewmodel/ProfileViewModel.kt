package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val user: UserModel) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object UpdateSuccess : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = ProfileUiState.Success(user)
            } else {
                _uiState.value = ProfileUiState.Error("User not found")
            }
        }
    }

    fun saveProfile(fullName: String, phone: String, university: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            authRepository.updateProfile(fullName, phone, university)
                .onSuccess {
                    _uiState.value = ProfileUiState.UpdateSuccess
                    loadProfile()
                }
                .onFailure { _uiState.value = ProfileUiState.Error(it.message ?: "Failed to update profile") }
        }
    }

    fun saveVehicleInfo(
        vehicleType: String,
        vehicleModel: String,
        vehicleColor: String,
        vehiclePlate: String
    ) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            authRepository.updateVehicleInfo(vehicleType, vehicleModel, vehicleColor, vehiclePlate)
                .onSuccess {
                    _uiState.value = ProfileUiState.UpdateSuccess
                    loadProfile()
                }
                .onFailure { _uiState.value = ProfileUiState.Error(it.message ?: "Failed to update vehicle info") }
        }
    }

    fun clearState() {
        _uiState.value = ProfileUiState.Idle
    }
}
