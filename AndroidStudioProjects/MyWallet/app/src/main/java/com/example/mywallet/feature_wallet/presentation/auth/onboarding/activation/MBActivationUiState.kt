package com.example.mywallet.feature_wallet.presentation.auth.onboarding.activation

data class MBActivationUiState(
    val voiceState: ActivationVoiceState? = null,
    val dataState: DataState? = null,
    val verifyDevice: Boolean = false
)

data class ActivationVoiceState(
    val welcomePrompt: Boolean = false,
    val promptPhoneNumber: Boolean = false,
    val promptFullName: Boolean = false,
    val promptEmailAddress: Boolean = false,
    val invalidEmailAddress: Boolean = false,
    val invalidPhoneNumber: Boolean = false,
    val invalidFullName: Boolean = false,
    val promptMessageSent: Boolean = false,
    val activateUserPrompt: Boolean = false
)

data class DataState(
    val isPhoneNumber: Boolean = false,
    val isFullName: Boolean = false,
    val isEmailAddress: Boolean = false,
)