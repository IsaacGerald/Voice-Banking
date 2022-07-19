package com.example.mywallet.feature_wallet.presentation.wallet.transaction

import com.example.mywallet.feature_wallet.domain.model.Transaction
import com.example.mywallet.feature_wallet.domain.model.Transfer

data class TransUiState(
    val currentBalance: String? = null,
    val transactions: List<Transaction>? = null,
    val voiceState: TransVoiceState? = null,
    val navigateToServiceTransfer: Transfer? = null

)

data class TransVoiceState(
    val isCurrentBalance: Boolean = false
)