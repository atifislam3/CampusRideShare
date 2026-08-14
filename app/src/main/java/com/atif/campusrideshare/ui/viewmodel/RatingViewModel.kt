package com.atif.campusrideshare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atif.campusrideshare.data.repository.AuthRepository
import com.atif.campusrideshare.data.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RatingUiState {
    object Idle : RatingUiState()
    object Loading : RatingUiState()
    object Success : RatingUiState()
    data class Error(val message: String) : RatingUiState()
}

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RatingUiState>(RatingUiState.Idle)
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    fun submitRating(
        ratedUid: String,
        rideId: String,
        stars: Int,
        review: String,
        raterRole: String
    ) {
        viewModelScope.launch {
            _uiState.value = RatingUiState.Loading
            
            val raterUid = authRepository.getCurrentUser()?.uid
            if (raterUid == null) {
                _uiState.value = RatingUiState.Error("User not authenticated")
                return@launch
            }

            ratingRepository.submitRating(
                ratedUid = ratedUid,
                raterUid = raterUid,
                rideId = rideId,
                stars = stars,
                review = review,
                raterRole = raterRole
            ).onSuccess {
                _uiState.value = RatingUiState.Success
            }.onFailure {
                _uiState.value = RatingUiState.Error(it.message ?: "Failed to submit rating")
            }
        }
    }

    fun clearState() {
        _uiState.value = RatingUiState.Idle
    }
}
