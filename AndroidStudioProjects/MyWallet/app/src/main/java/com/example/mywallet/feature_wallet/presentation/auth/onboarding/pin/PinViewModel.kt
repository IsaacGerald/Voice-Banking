package com.example.mywallet.feature_wallet.presentation.auth.onboarding.pin

import android.util.Log
import androidx.lifecycle.*
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserPin
import com.example.mywallet.feature_wallet.presentation.auth.login.pin.VoicePinState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PinViewModel(
    private val saveUserPin: SaveUserPin,
    private val saveUser: SaveUser,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = PinViewModel::class.java.simpleName
    private var _user = MutableLiveData<User>()
    private var _voiceState = MutableLiveData<VoicePinState>()
    private var _pin = MutableLiveData<Int>(0)
    private var _confirmPin = MutableLiveData<Int>(0)
    private var _pinUiEvent = Channel<UIEvent>()

    val pinUiEvent = _pinUiEvent.receiveAsFlow()

    val user: LiveData<User>
        get() = _user

    val pin: LiveData<Int>
        get() = _pin

    val confirmPin: LiveData<Int>
        get() = _confirmPin

    val voiceState: LiveData<VoicePinState>
        get() = _voiceState


    fun setVoiceState(voiceState: VoicePinState) {
        _voiceState.postValue(voiceState)
    }

    fun setUser(user: User) {
        _user.value = user
    }

    fun savePin(p: String, isConfirmPin: Boolean) {

        if (isInteger(p)) {

            if (!isConfirmPin) {
                _pin.postValue(Integer.parseInt(p))
                viewModelScope.launch {
                    _pinUiEvent.send(UIEvent.ConfirmPin)
                }
            } else {

                validatePin(Integer.parseInt(p))
            }

        } else {
            viewModelScope.launch {
                _pinUiEvent.send(UIEvent.PinIsInvalid(isConfirmPin))
            }
        }
    }

    private fun isInteger(str: String) = str.toIntOrNull()?.let { true } ?: false


    private fun validatePin(pinConfirm: Int) {
        if (pin.value != null && pinConfirm > 0) {
            val pin1 = pin.value

            viewModelScope.launch {

                if (pin1 == pinConfirm) {
                    _pinUiEvent.send(UIEvent.CompleteVerification)

                    savePinToDatastore(pinConfirm)
                    //saveUser(pinConfirm)
                } else {
                    _pin.postValue(0)
                    _confirmPin.postValue(0)
                    _pinUiEvent.send(UIEvent.PinDoesNotMatch)
                }
            }
        } else {
            return
        }


    }

    private fun saveUser(pinConfirm: Int) {

        val newUser = user.value?.copy(pin = pinConfirm)
        Log.d(TAG, "saveUser: $newUser")
        viewModelScope.launch {
            if (newUser != null) {
                saveUser(newUser)
            }
        }
    }

    private fun savePinToDatastore(pinConfirm: Int) {
        viewModelScope.launch {
            saveUserPin(pinConfirm)
        }
    }


    sealed class UIEvent {
        object PinDoesNotMatch : UIEvent()
        object CompleteVerification : UIEvent()
        data class PinIsInvalid(val isConfirmPin: Boolean) : UIEvent()
        object ConfirmPin : UIEvent()


    }

}