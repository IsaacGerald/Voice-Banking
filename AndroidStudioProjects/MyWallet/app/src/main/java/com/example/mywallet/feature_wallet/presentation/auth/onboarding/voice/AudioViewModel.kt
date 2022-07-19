package com.example.mywallet.feature_wallet.presentation.auth.onboarding.voice

import androidx.lifecycle.*
import com.example.mywallet.feature_wallet.domain.model.RandomStats
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceRegistration
import com.example.mywallet.feature_wallet.domain.use_case.SaveAudioFile
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AudioViewModel(
    private val getVoiceRegistration: GetVoiceRegistration,
    private val saveAudioFile: SaveAudioFile,
    private val saveUser: SaveUser,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _statementIndex = MutableLiveData(0)
    private var _user = MutableLiveData<User>()
    private var _statement = MutableLiveData<String>()
    private var _voiceState = MutableLiveData<VoiceAudioState>()
    private var _audioPath = MutableLiveData<List<String>>()
    private var _audioUiEvent = Channel<AudioUiEvent>()

    val user: LiveData<User>
        get() = _user

    val audioUiEvent = _audioUiEvent.receiveAsFlow()

    val audioPath: LiveData<List<String>>
        get() = _audioPath

    val voiceState: LiveData<VoiceAudioState>
        get() = _voiceState

    val statement: LiveData<String>
        get() = _statement

    val statementIndex: LiveData<Int>
        get() = _statementIndex


    private val randomStats = mutableListOf<RandomStats>(
        RandomStats("My voice is My password"),
        RandomStats("I love my bank"),
        RandomStats("Banking  is made easy"),
    )

    init {
        getStatement()
    }

    fun getStatement() {
        _statement.postValue(randomStats[statementIndex.value!!].statement)
    }

    fun setVoiceState(state: VoiceAudioState) {
        _voiceState.postValue(state)
    }

    fun saveAudio(audio: String) {
        val path = audioPath.value?.toMutableList()
        path?.let {
            path.add(audio)
            _audioPath.postValue(it)
        }

    }

    fun saveUser(user: User) {
        _user.value = user
    }

    fun getVoiceValidation(file: String) {
        viewModelScope.launch {
            saveFileToUser(file)
            _audioUiEvent.send(AudioUiEvent.ShowProgressBar)
        }

//        getVoiceRegistration(File(file)).onEach { result ->
//            when(result){
//                is Resource.Loading -> {
//                    _audioUiEvent.send(AudioUiEvent.ShowProgressBar)
//                }
//                is Resource.Success -> {
//                    _audioUiEvent.send(AudioUiEvent.VoiceRegistrationSuccessFull)
//                }
//                is Resource.Error -> {
//                    _audioUiEvent.send(AudioUiEvent.VoiceRegistrationFailed)
//                }
//            }
//
//        }.launchIn(viewModelScope)
    }

    private fun saveFileToUser(file: String) {
        val newUser = _user.value?.copy(filePath = file)
        _user.value = newUser!!
    }


    fun nextStatement() {
        var number: Int = _statementIndex.value ?: 0
        number++

        if (number <= 3) {
            _statementIndex.value = number
        } else {
            _statementIndex.value = 0
            return
        }


    }

    fun resetIndex() {
        _statementIndex.postValue(0)
    }

    sealed class AudioUiEvent {
        object ShowProgressBar : AudioUiEvent()
        object VoiceRegistrationFailed : AudioUiEvent()
        object VoiceRegistrationSuccessFull : AudioUiEvent()
    }

}