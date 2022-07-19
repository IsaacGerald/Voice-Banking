package com.example.mywallet.feature_wallet.presentation.auth.onboarding.signup

import android.util.Log
import androidx.lifecycle.*
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val saveUserCredentials: SaveUserCredentials,
    private val saveUser: SaveUser,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = SignUpViewModel::class.java.simpleName
    private val _passportId = MutableLiveData<String>()
    private val _user = MutableLiveData<User>()

    private var _signUpUiState = MutableStateFlow(SignUpUiState())

    val signUpUiState: StateFlow<SignUpUiState> = _signUpUiState


    val passportId: LiveData<String>
        get() = _passportId

    val user: LiveData<User>
        get() = _user



    private fun saveUserCredential(
        id: String? = null,
        phoneNumber: String? = null,
        userName: String? = null,
        passport: String? = null,
        account: List<String> = emptyList()
    ) = viewModelScope.launch {
        val newUser = User(
            id = user.value?.id,
            phoneNumber = user.value?.phoneNumber,
            userName = user.value?.userName,
            passport = user.value?.passport
        )

        if (id != null) {
            if (isInteger(id)) {
                newUser.nationalId = id
                onEvent(SignupUiEvent.GetPhoneNumber)
            } else {
                _signUpUiState.value =
                    signUpUiState.value.copy(voiceState = SignupVoiceState(idIsInvalid = true))
            }
        } else if (passport != null) {
            if (isInteger(passport)) {
                newUser.passport = passport
                onEvent(SignupUiEvent.GetPhoneNumber)
            } else {
                _signUpUiState.value =
                    signUpUiState.value.copy(voiceState = SignupVoiceState(passportIsInvalid = true))
            }
        } else if (phoneNumber != null) {
            if (isInteger(phoneNumber)) {
                onEvent(SignupUiEvent.GetUserName)
                newUser.phoneNumber = Integer.parseInt(phoneNumber)
            } else {
                _signUpUiState.value =
                    signUpUiState.value.copy(voiceState = SignupVoiceState(phoneNumberIsInvalid = true))
            }
        } else if (!userName.isNullOrBlank()) {
            newUser.userName = userName
            verifyUserCredentials()
        }
//        else if (!account.isNullOrEmpty()) {
//            newUser.accounts?.toMutableList()?.add(account[0])
//        }

        _user.postValue(newUser)


        Log.d(TAG, "saveUserCredential: ${newUser.passport}")


    }

    fun onEvent(event: SignupUiEvent) {
        when (event) {
            is SignupUiEvent.InitVoicePrompt -> {
                _signUpUiState.value =
                    signUpUiState.value.copy(voiceState = SignupVoiceState(isPassportOrId = true))
            }
            is SignupUiEvent.GetPhoneNumber -> {
                _signUpUiState.value =
                    signUpUiState.value.copy(
                        credentialState = CredentialState(isPhoneNumber = true),
                        voiceState = SignupVoiceState(isPhoneNumber = true)
                    )
            }
            is SignupUiEvent.GetUserName -> {
                _signUpUiState.value =
                    signUpUiState.value.copy(
                        credentialState = CredentialState(isUserName = true),
                        voiceState = SignupVoiceState(isUsername = true)
                    )
            }
            is SignupUiEvent.SaveUserCredentials -> {
                saveUserCredential(
                    id = event.id,
                    phoneNumber = event.phoneNumber,
                    userName = event.userName,
                    passport = event.passport,
                    account = event.account
                )
            }
            is SignupUiEvent.GetPassportOrId -> {
                _passportId.value = event.passportOrId
                _signUpUiState.value =
                    signUpUiState.value.copy(
                        credentialState = CredentialState(isPassportOrId = true),
                        passportOrId = event.passportOrId,
                        voiceState = SignupVoiceState(enterPassportOrId = true)
                    )
            }
        }
    }

    private fun isInteger(str: String) = str.toIntOrNull()?.let { true } ?: false

    private fun verifyUserCredentials() {
        saveUserToDatastore()
        _signUpUiState.value =
            signUpUiState.value.copy(voiceState = SignupVoiceState(confirmAccount = true))

    }


    private fun saveUserToDatastore() {
        val newUser = User(
            id = user.value?.id,
            phoneNumber = user.value?.phoneNumber,
            userName = user.value?.userName,
            passport = user.value?.passport,
        )
        viewModelScope.launch {
            saveUserCredentials(newUser)
        }

    }

    sealed class SignupUiEvent {
        object InitVoicePrompt : SignupUiEvent()
        object GetPhoneNumber : SignupUiEvent()
        object GetUserName : SignupUiEvent()
        data class GetPassportOrId(val passportOrId: String) : SignupUiEvent()
        data class SaveUserCredentials(
            val id: String? = null,
            val phoneNumber: String? = null,
            val userName: String? = null,
            val passport: String? = null,
            val account: List<String> = emptyList()
        ) : SignupUiEvent()
    }


}