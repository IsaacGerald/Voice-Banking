package com.example.mywallet.feature_wallet.presentation.auth.onboarding.splashscreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SplashFragment1ViewModel : ViewModel() {
   private val _splashUiState = MutableStateFlow(Splash1UiState())

    val splashUiState: StateFlow<Splash1UiState> = _splashUiState



    fun onEvent(event: Splash1Event){
        when(event){
            is Splash1Event.WelcomeUserPrompt -> {
                _splashUiState.value = splashUiState.value.copy(voiceState = Splash1VoiceState(welcomePrompt = true))
            }


        }
    }














    sealed class Splash1Event {
        object WelcomeUserPrompt : Splash1Event()
    }


}