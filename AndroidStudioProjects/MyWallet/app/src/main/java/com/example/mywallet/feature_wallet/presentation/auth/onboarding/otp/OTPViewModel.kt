package com.example.mywallet.feature_wallet.presentation.auth.onboarding.otp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OTPViewModel: ViewModel() {
   private  val _uiState = MutableStateFlow(OTPUiState())

    val uiState: StateFlow<OTPUiState> = _uiState


    fun onEvent(event: OTPUiEvent){
        when(event){
            is OTPUiEvent.GetOTPPrompt -> {
                _uiState.value = uiState.value.copy(voiceState = OtpVoiceState(getOtpMessagePrompt = true))
            }
            is OTPUiEvent.DeviceVerificationIsSuccessFull -> {
                _uiState.value = uiState.value.copy(voiceState = OtpVoiceState(verificationSuccessFull = true))
            }

        }
    }




    sealed class OTPUiEvent{
        object GetOTPPrompt: OTPUiEvent()
        object DeviceVerificationIsSuccessFull: OTPUiEvent()
    }
}