package com.example.mywallet.feature_wallet.presentation.auth.login.face

import androidx.lifecycle.*
import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialLogin
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserLoginStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class FaceLoginViewModel(
    private val getFacialLogin: GetFacialLogin,
    private val saveUserLoginStatus: SaveUserLoginStatus,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _faceUIState = MutableStateFlow(FaceUIState())

    val faceUiState: StateFlow<FaceUIState> = _faceUIState

    private var model: FaceModel? = null


    fun onEvent(event: FaceLoginUiEvent) {
        when (event) {
            is FaceLoginUiEvent.GetValidation -> {
                getFaceValidation(event.file)
            }
            is FaceLoginUiEvent.CapturePhoto -> {
                _faceUIState.value =
                    faceUiState.value.copy(
                        isLoading = false,
                        error = null,
                        faceModel = null,
                        voiceState = FaceVoiceState(isPromptToCapturePhoto = true)
                    )
            }
            is FaceLoginUiEvent.InitVoicePrompt -> {
                _faceUIState.value =
                    faceUiState.value.copy(
                        voiceState = FaceVoiceState(isFacialRecognition = true)
                    )
            }
        }
    }

    private fun getFaceValidation(it: File) {
        getFacialLogin(it).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    if (result.data?.status!!) {
                        model = result.data
                        _faceUIState.value =
                            _faceUIState.value.copy(
                                isLoading = false,
                                faceModel = result.data,
                                voiceState = FaceVoiceState(completeVerification = true)
                            )
                    } else {
                        _faceUIState.value =
                            _faceUIState.value.copy(
                                isLoading = false,
                                faceModel = result.data,
                                voiceState = FaceVoiceState(isInvalidImage = true),
                            )
                    }

                }
                is Resource.Loading -> {
                    _faceUIState.value = faceUiState.value.copy(
                        isLoading = true,
                        faceModel = null,
                        voiceState = null,
                        error = result.message
                    )
                }
                is Resource.Error -> {
                    _faceUIState.value = faceUiState.value.copy(
                        isLoading = false,
                        faceModel = null,
                        voiceState = FaceVoiceState(confirmRequest = true),
                        error = result.message
                    )
                }
            }
        }.launchIn(viewModelScope)


    }

    fun setUserIsLoggedIn(condition: String) {
        viewModelScope.launch {

        }
    }

    sealed class FaceLoginUiEvent {
        data class GetValidation(val file: File) : FaceLoginUiEvent()
        object CapturePhoto : FaceLoginUiEvent()
        object InitVoicePrompt: FaceLoginUiEvent()
    }

}