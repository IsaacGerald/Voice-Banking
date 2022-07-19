package com.example.mywallet.feature_wallet.presentation.auth.onboarding.face

import androidx.lifecycle.*
import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialValidation
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserOnBoardingStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

class FaceValidationViewModel(
    private val getFacialValidation: GetFacialValidation,
    private val saveUserOnBoardingStatus: SaveUserOnBoardingStatus,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _faceUiEvent = Channel<UIEvent>()
    private var _voiceFaceValidationState = MutableLiveData<VoiceFaceValidationState>()

    val voiceFaceValidationState: LiveData<VoiceFaceValidationState>
        get() = _voiceFaceValidationState

    val faceUIEvent = _faceUiEvent.receiveAsFlow()


    fun getFaceValidation(file: File) {
        getFacialValidation(file).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _faceUiEvent.send(UIEvent.IsLoading(result.data))
                }

                is Resource.Success -> {
                    _faceUiEvent.send(UIEvent.Success(result.data))
                }

                is Resource.Error -> {
                    _faceUiEvent.send(UIEvent.Error(result.message))
                }

            }
        }.launchIn(viewModelScope)

    }

    fun setUserOnBoardingStatus(){
        viewModelScope.launch {
            saveUserOnBoardingStatus(false)
        }
    }


    fun setVoiceFaceValidationState(voiceFaceValidationState: VoiceFaceValidationState) {
        _voiceFaceValidationState.postValue(voiceFaceValidationState)
    }

    sealed class UIEvent {
        data class IsLoading(val data: FaceModel? = null) : UIEvent()
        data class Error(val message: String?) : UIEvent()
        data class Success(val model: FaceModel?) : UIEvent()
        object SetUserOnBoardingStatus: UIEvent()
    }
}