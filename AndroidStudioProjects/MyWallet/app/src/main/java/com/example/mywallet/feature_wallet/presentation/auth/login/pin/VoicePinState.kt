package com.example.mywallet.feature_wallet.presentation.auth.login.pin

data class VoicePinState(
    val enterPin: Boolean = false,
    val confirmPin: Boolean = false,
    val completeValidation: Boolean = false,
    val isIncorrectPin: Boolean = false,
    val isInvalidPin: Boolean = false
)