package com.example.mywallet.feature_wallet.presentation.auth.login.pin

import android.util.Log
import androidx.lifecycle.*
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.GetUserPin
import com.example.mywallet.feature_wallet.domain.use_case.GetUsers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginPinViewModel(
    private val getUserPin: GetUserPin,
    private val getUsers: GetUsers,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = LoginPinViewModel::class.java.simpleName

    private var _voiceState = MutableLiveData<VoicePinState>()
    private var _pin = MutableLiveData<Int>(0)
    private var _confirmPin = MutableLiveData<Int>(0)
    private var _pinUiEvent = Channel<UIEvent>()
    private var _users = MutableLiveData<List<User>>()

    val users: LiveData<List<User>>
        get() = _users

    val pinUiEvent = _pinUiEvent.receiveAsFlow()

    val pin: LiveData<Int>
        get() = _pin

    val confirmPin: LiveData<Int>
        get() = _confirmPin

    val voiceState: LiveData<VoicePinState>
        get() = _voiceState

    init {
        getLoggedInUsers()
    }

    private fun getLoggedInUsers() {
        getUsers().onEach { users ->
            _users.value = users
        }.launchIn(viewModelScope)
    }

    fun setVoiceState(voiceState: VoicePinState) {
        _voiceState.postValue(voiceState)
    }

    fun savePin(p: String) {

        if (isInteger(p)) {
            _pin.postValue(Integer.parseInt(p))
            validatePin(Integer.parseInt(p))
        } else {
            viewModelScope.launch {
                _pinUiEvent.send(UIEvent.PinIsInvalid)
            }
        }
    }


    private fun isInteger(str: String) = str.toIntOrNull()?.let { true } ?: false


    private fun validatePin(pin: Int) {

        viewModelScope.launch {
            _pinUiEvent.send(UIEvent.ShowProgressBar)

            val myPin = users.value?.get(0)!!.pin
            Log.d(TAG, "validatePin: ${users.value!![0]}")
            if (myPin == pin) {
                _pinUiEvent.send(UIEvent.CompleteVerification)
            } else {
                _pinUiEvent.send(UIEvent.PinDoesNotMatch)
            }
        }


    }


    sealed class UIEvent {
        object PinDoesNotMatch : UIEvent()
        object CompleteVerification : UIEvent()
        object PinIsInvalid : UIEvent()
        object ConfirmPin : UIEvent()
        object ShowProgressBar : UIEvent()


    }

}