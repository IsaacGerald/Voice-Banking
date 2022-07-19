package com.example.mywallet.feature_wallet.presentation.auth.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToFaceFragment: Boolean = false,
    val loginVoiceState: LoginVoiceState? = null,
)


data class LoginVoiceState(
    val promptSignInOrSignup: Boolean = false,
    val promptToEnterPasswordOrId: Boolean = false,
    val enterPasswordOrId: Boolean = false,
    val enterPhoneNumber: Boolean = false,
    val invalidPassportOrId: Boolean = false,
    val invalidPhoneNumber: Boolean = false,
    val voiceLoginSuccessful: Boolean = false,
    val voiceLoginFailed: Boolean = false,
    val welcomePrompt: Boolean = false
)


