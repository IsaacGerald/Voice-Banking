package com.example.mywallet.feature_wallet.presentation.auth.login.welcomescreen

data class WelcomeLoginUiState(
    val isNewUser: Boolean? = null,
    val voiceState: WelcomeLoginVoiceState? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToFaceFragment: Boolean = false,
)

data class WelcomeLoginVoiceState(
    val welcomePrompt: Boolean = true,
    val voiceLoginSuccessful: Boolean = true
)