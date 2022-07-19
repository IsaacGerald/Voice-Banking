package com.example.mywallet.feature_wallet.presentation.auth.login

import androidx.lifecycle.*
import com.example.mywallet.core.util.Constants
import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.QuestionLogin
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.GetAudioFile
import com.example.mywallet.feature_wallet.domain.use_case.GetIsNewUser
import com.example.mywallet.feature_wallet.domain.use_case.GetUsers
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceLogin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class LoginViewModel(
    private val getVoiceLogin: GetVoiceLogin,
    private val getAudioFile: GetAudioFile,
    private val getUsers: GetUsers,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    //  private var _loginEventChannel = Channel<LoginUiState>()
    private var _loggedInUsers = MutableLiveData<List<User>>()
    private var _loginUiState = MutableStateFlow(LoginUiState())

    private var _voiceLoginState = MutableLiveData<LoginUiState>()
    private var _isPassport = MutableLiveData<Boolean>()
    private var _user = MutableLiveData<User>()
    private var _enr_audio = MutableLiveData<String>()

    // val loginEvent = _loginEventChannel.receiveAsFlow()
    val loginUiState: StateFlow<LoginUiState> = _loginUiState

    val loggedInUsers: LiveData<List<User>>
    get() = _loggedInUsers

    val enr_audio: LiveData<String>
        get() = _enr_audio

    val audio = savedStateHandle.getLiveData<String>(Constants.AUDIO_1)

    val user: LiveData<User>
        get() = _user

    val isPassport: LiveData<Boolean>
        get() = _isPassport

    val voiceState: LiveData<LoginUiState>
        get() = _voiceLoginState

    init {
        getEnrollmentAudio()
        getLoggedUsers()
    }

    private fun getLoggedUsers() {
        getUsers().onEach { users ->
            _loggedInUsers.value = users
        }.launchIn(viewModelScope)
    }


    private val questions = mutableListOf<QuestionLogin>(
        QuestionLogin("What is your id number", "332145"),
        QuestionLogin("What is your passport Number", "332145"),
        QuestionLogin("What is your phoneNumber", "0704018825")
    )

    private fun saveUser(passportOrId: String? = null, phoneNumber: String? = null) =
        viewModelScope.launch {
            val newUser = User(
                phoneNumber = user.value?.phoneNumber,
                passport = user.value?.passport,
                nationalId = user.value?.nationalId
            )

            if (passportOrId != null) {
                if (isInteger(passportOrId)) {
                    newUser.passport = if (isPassport.value == true) passportOrId else ""
                    newUser.nationalId = if (isPassport.value == false) passportOrId else ""
                    onEvent(LoginUiEvent.GetPhoneNumber)
                    // _loginUiState.value = LoginUiState.GetPhoneNumber
                } else {
                    // _loginUiState.value = LoginUiState.PassportOrIdIsInvalid
                    onEvent(LoginUiEvent.PassportOrIdIsInvalid)
                }
            } else if (phoneNumber != null) {
                if (isInteger(phoneNumber)) {
                    newUser.phoneNumber = Integer.parseInt(phoneNumber)

                } else {
                    // _loginUiState.value = LoginUiState.PhoneNumberIsInvalid
                    onEvent(LoginUiEvent.PhoneNumberIsInvalid)
                }
            }

            _user.postValue(newUser)

        }


    fun saveAudio(filePath: String, key: String) {
        savedStateHandle[key] = filePath
    }

    private fun isInteger(str: String) = str.toIntOrNull()?.let { true } ?: false


    fun setIsPassport(isPassport: Boolean) {
        _isPassport.postValue(isPassport)
    }


    private fun randomQuestion(): QuestionLogin {
        val number = (0..2).shuffled().last()
        return questions[number]
    }


    private fun getEnrollmentAudio() {
        getAudioFile().onEach { filePath ->
            _enr_audio.postValue(filePath)
        }
    }

    private fun validateUserVoice(enroll: String) {
        val filePath = loggedInUsers.value?.get(0)?.filePath

        getVoiceLogin(File(filePath!!), File(enroll)).onEach { result ->
            when (result) {
                is Resource.Loading -> {

                    _loginUiState.value =
                        loginUiState.value.copy(isLoading = true, loginVoiceState = null)
                }
                is Resource.Success -> {
                    if (result.data!![0]) {
                        _loginUiState.value =
                            loginUiState.value.copy(
                                isLoading = false,
                                loginVoiceState = LoginVoiceState(voiceLoginSuccessful = true)
                            )
                    } else {
                        _loginUiState.value = loginUiState.value.copy(
                            isLoading = false,
                            navigateToFaceFragment = true,
                            loginVoiceState = null
                        )
                    }

                }
                is Resource.Error -> {

                    _loginUiState.value =
                        loginUiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            loginVoiceState = null
                        )
                }

            }
        }.launchIn(viewModelScope)


    }


    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.GetPhoneNumber -> {
                _loginUiState.value =
                    _loginUiState.value.copy(loginVoiceState = LoginVoiceState(enterPhoneNumber = true))
            }

            is LoginUiEvent.AsKForPassportOrId -> {
                _loginUiState.value = loginUiState.value.copy(
                    loginVoiceState = LoginVoiceState(
                        promptToEnterPasswordOrId = true
                    )
                )
            }

            is LoginUiEvent.SaveUser -> {
                saveUser(event.passportOrId, event.phoneNumber)
            }
            is LoginUiEvent.PassportOrIdIsInvalid -> {
                _loginUiState.value =
                    loginUiState.value.copy(loginVoiceState = LoginVoiceState(invalidPassportOrId = true))
            }
            is LoginUiEvent.PhoneNumberIsInvalid -> {
                _loginUiState.value =
                    loginUiState.value.copy(loginVoiceState = LoginVoiceState(invalidPhoneNumber = true))
            }

            is LoginUiEvent.ValidateUsersVoice -> {
                validateUserVoice(event.filePath)
            }
            is LoginUiEvent.GetWelcomePrompt -> {
                _loginUiState.value =
                    loginUiState.value.copy(loginVoiceState = LoginVoiceState(welcomePrompt = true))
            }

        }
    }

    sealed class LoginUiEvent {
        object GetWelcomePrompt: LoginUiEvent()
        object GetPhoneNumber : LoginUiEvent()
        object PassportOrIdIsInvalid : LoginUiEvent()
        object PhoneNumberIsInvalid : LoginUiEvent()
        object AsKForPassportOrId : LoginUiEvent()
        data class ValidateUsersVoice(val filePath: String) : LoginUiEvent()
        data class SaveUser(val passportOrId: String? = null, val phoneNumber: String? = null) :
            LoginUiEvent()
    }


}


