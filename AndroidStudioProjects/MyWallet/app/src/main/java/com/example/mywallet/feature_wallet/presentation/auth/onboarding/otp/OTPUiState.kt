package com.example.mywallet.feature_wallet.presentation.auth.onboarding.otp

data class OTPUiState(
    val voiceState: OtpVoiceState? = null
)

data class OtpVoiceState(
    val getOtpMessagePrompt: Boolean = false,
    val verificationSuccessFull: Boolean = false
)