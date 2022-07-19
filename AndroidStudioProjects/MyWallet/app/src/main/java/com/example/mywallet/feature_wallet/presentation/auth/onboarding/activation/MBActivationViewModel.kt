package com.example.mywallet.feature_wallet.presentation.auth.onboarding.activation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.mywallet.core.util.isInteger
import com.example.mywallet.feature_wallet.domain.model.UserDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MBActivationViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(MBActivationUiState())
    private var _userInfo = mutableStateOf(UserDetail())
    val uiState: StateFlow<MBActivationUiState> = _uiState
    val userInfo: State<UserDetail> = _userInfo


    fun onEvent(event: ActivationEvent) {

        when (event) {
            is ActivationEvent.GetScreenDescription -> {
                _uiState.value =
                    uiState.value.copy(voiceState = ActivationVoiceState(welcomePrompt = true))
            }
            is ActivationEvent.SaveData -> {
                saveData(event.data)
            }
            is ActivationEvent.ActivateUser -> {
                _uiState.value =
                    uiState.value.copy(voiceState = ActivationVoiceState(activateUserPrompt = true))
            }
            is ActivationEvent.UpdateDataState -> {
                updateDataState(event.dataState)
            }
        }
    }

    private fun updateDataState(dataState: DataState) {
        _uiState.value = uiState.value.copy(dataState = dataState)
    }

    private fun saveData(data: String?) {
        val dataState = uiState.value.dataState
        if (dataState != null) {
            when {
                dataState.isPhoneNumber -> {
                    if (data != null) {
                        if (isInteger(data)) {
                            _uiState.value = uiState.value.copy(
                                voiceState = ActivationVoiceState(promptFullName = true)
                            )
                        } else {
                            _uiState.value = uiState.value.copy(
                                voiceState = ActivationVoiceState(invalidPhoneNumber = true)
                            )
                        }
                    }
                }
                dataState.isFullName -> {
                    if (data != null) {
                        _userInfo.value = userInfo.value.copy(fullName = data)
                        _uiState.value = uiState.value.copy(verifyDevice = true)
                    } else {
                        _uiState.value = uiState.value.copy(
                            voiceState = ActivationVoiceState(invalidFullName = true)
                        )
                    }
                }
                dataState.isEmailAddress -> {
                    if (data != null) {
                        _userInfo.value = userInfo.value.copy(emailAddress = data)
                        onEvent(ActivationEvent.ActivateUser)
                    } else {
                        //TODO
                    }
                }
            }
        }
    }


    sealed class ActivationEvent {
        object GetScreenDescription : ActivationEvent()
        object ActivateUser : ActivationEvent()
        data class UpdateDataState(val dataState: DataState) : ActivationEvent()
        data class SaveData(val data: String?) : ActivationEvent()
    }

}