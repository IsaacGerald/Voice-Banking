package com.example.mywallet.feature_wallet.presentation.auth.login.welcomescreen

import android.util.Log
import androidx.lifecycle.*
import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.GetIsNewUser
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceLogin
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginUiState
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginViewModel
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginVoiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class WelcomeLoginViewModel(
    private val getIsNewUser: GetIsNewUser,
    private val getVoiceLogin: GetVoiceLogin,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = WelcomeLoginViewModel::class.java.simpleName
    private var _uiState = MutableStateFlow(WelcomeLoginUiState())
    private var _loggedInUsers = MutableLiveData<List<User>>()

    val loggedInUsers: LiveData<List<User>>
        get() = _loggedInUsers

    val uiState: StateFlow<WelcomeLoginUiState> = _uiState


    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.GetIsNewUser -> {
                viewModelScope.launch {
                    getIsNewUser().collect { isNewUser ->
                        Log.d(TAG, "onEvent: $isNewUser")
                        _uiState.value = uiState.value.copy(isNewUser = isNewUser)
                    }
                }
            }
            is UiEvent.GetWelcomePrompt -> {
                _uiState.value =
                    uiState.value.copy(voiceState = WelcomeLoginVoiceState(welcomePrompt = true))
            }
            is UiEvent.ValidateUsersVoice -> {
                validateUserVoice(event.filePath)
            }
        }
    }

    private fun validateUserVoice(filePath: String) {
        //val filePath = loggedInUsers.value?.get(0)?.filePath

        getVoiceLogin(File(filePath), File(filePath)).onEach { result ->
            when (result) {
                is Resource.Loading -> {

                    _uiState.value =
                        uiState.value.copy(isLoading = true, voiceState = null)
                }
                is Resource.Success -> {
                    if (result.data!![0]) {
                        _uiState.value =
                            uiState.value.copy(
                                isLoading = false,
                                voiceState = WelcomeLoginVoiceState(voiceLoginSuccessful = true)
                            )
                    } else {
                        _uiState.value = uiState.value.copy(
                            isLoading = false,
                            navigateToFaceFragment = true,
                            voiceState = null
                        )
                    }

                }
                is Resource.Error -> {

                    _uiState.value =
                        uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            voiceState = null
                        )
                }

            }
        }.launchIn(viewModelScope)


    }


    sealed class UiEvent {
        object GetIsNewUser : UiEvent()
        object GetWelcomePrompt : UiEvent()
        data class ValidateUsersVoice(val filePath: String) : UiEvent()
    }


}