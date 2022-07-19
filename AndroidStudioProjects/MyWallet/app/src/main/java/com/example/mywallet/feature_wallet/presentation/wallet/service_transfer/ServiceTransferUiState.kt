package com.example.mywallet.feature_wallet.presentation.wallet.service_transfer


data class ServiceTransferUiState(
    val voiceState: STVoiceState? = null,
    val amount: Int? = 0,
    val source: String? = null
)

data class STVoiceState(
    val validateReceiveAmount: Boolean = false,
    val validateSourceOfTransfer: Boolean = false,
    val confirmReceiveTransfer: Boolean = false
)