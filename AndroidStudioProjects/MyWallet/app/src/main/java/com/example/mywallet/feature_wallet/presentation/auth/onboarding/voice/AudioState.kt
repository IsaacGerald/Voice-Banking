package com.example.mywallet.feature_wallet.presentation.auth.onboarding.voice

data class VoiceAudioState(
    val promptRepeatWords: Boolean = false,
    val repeatTextWords: Boolean = false,
    val endVoiceAuthentication: Boolean = false,
    val voiceRegistrationFailed: Boolean = false,


)

