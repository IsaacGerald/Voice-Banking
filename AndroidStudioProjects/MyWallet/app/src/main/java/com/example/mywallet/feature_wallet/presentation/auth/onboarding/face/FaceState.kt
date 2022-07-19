package com.example.mywallet.feature_wallet.presentation.auth.onboarding.face

data class VoiceFaceValidationState(
    val isPromptToCapturePhoto: Boolean = false,
    val confirmRequest: Boolean = false,
    var completeVerification: Boolean = false,
    var isValidating: Boolean = false,
    var imageIsInvalid: Boolean = false

)