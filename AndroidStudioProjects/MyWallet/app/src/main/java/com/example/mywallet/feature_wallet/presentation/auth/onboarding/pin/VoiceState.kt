package com.example.mywallet.feature_wallet.presentation.auth.onboarding.pin

data class VoiceState(
    val enterPin: Boolean = false,
    val confirmPin: Boolean = false,
    val completeValidation: Boolean = false,
    val isIncorrectPin: Boolean = false,
    val isInvalidPin: Boolean = false
)