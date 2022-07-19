package com.example.mywallet.feature_wallet.presentation.auth.onboarding.splashscreen

data class Splash1UiState(
    val voiceState: Splash1VoiceState? = null
)


data class Splash1VoiceState(
    val welcomePrompt: Boolean = false

)