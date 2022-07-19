package com.example.mywallet.feature_wallet.presentation.ui

sealed class VoiceTransactionState {
    data class ShowCurrentBalance(val showCurrentBalance: Boolean = false) : VoiceTransactionState()
    data class ValidateAmountReceived(val validateAmountReceived: Boolean = false) :
        VoiceTransactionState()

    data class ValidateSourceOfTransfer(val validateSourceOfTransfer: Boolean = false) :
        VoiceTransactionState()

    data class ConfirmReceiveTransfer(val confirmReceiveTransfer: Boolean = false) :
        VoiceTransactionState()
}







