package com.example.mywallet.feature_wallet.presentation.auth.login.face

import com.example.mywallet.feature_wallet.domain.model.FaceModel

data class FaceUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val faceModel: FaceModel? = null,
    val voiceState: FaceVoiceState? = null

)


data class FaceVoiceState(
    val isPromptToCapturePhoto: Boolean = false,
    val confirmRequest: Boolean = false,
    var completeVerification: Boolean = false,
    var isValidating: Boolean = false,
    var isInvalidImage: Boolean = false,
    var isFacialRecognition: Boolean = false
)