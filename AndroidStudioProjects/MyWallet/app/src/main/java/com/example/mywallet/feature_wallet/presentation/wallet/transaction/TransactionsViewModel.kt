package com.example.mywallet.feature_wallet.presentation.wallet.transaction

import android.os.Bundle
import androidx.lifecycle.*
import com.example.mywallet.core.util.Constants
import com.example.mywallet.feature_wallet.domain.model.Transfer
import com.example.mywallet.feature_wallet.domain.use_case.GetCurrentBalance
import com.example.mywallet.feature_wallet.domain.use_case.GetTransactions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionsViewModel(
    val savedStateHandle: SavedStateHandle,
    val getTransactions: GetTransactions,
    val getCurrentBalance: GetCurrentBalance
) : ViewModel() {
    private val TAG = TransactionsViewModel::class.java.simpleName

    private val _transactionUiState = MutableStateFlow(TransUiState())

    val transUiState: StateFlow<TransUiState> = _transactionUiState


    init {
        onEvent(TransUiEvent.GetCurrentBalance)
        onEvent(TransUiEvent.GetTransactions)
    }


    private fun handleArguments(args: Bundle) = viewModelScope.launch {

        val feature = args[Constants.FEATURE]
        val thing = args[Constants.NAME]
        val invoice = args[Constants.FOR_SERVICE_NAME]
        val transfer = args[Constants.TRANSFER_MODE]

        when {
            feature != null -> {
                _transactionUiState.value =
                    transUiState.value.copy(voiceState = TransVoiceState(isCurrentBalance = true))

            }
            thing != null -> {

            }
            invoice != null -> {

            }
            transfer != null -> {
                val transferModel = Transfer(
                    originName = args[Constants.ORIGIN_NAME].toString(),
                    destinationName = args[Constants.DESTINATION_NAME].toString(),
                    value = args[Constants.VALUE].toString(),
                    currency = args[Constants.CURRENCY].toString(),
                    originProviderName = args[Constants.ORIGIN_PROVIDER_NAME].toString(),
                    originDestinationName = args[Constants.DESTINATION_PROVIDER_NAME].toString(),
                    transferMode = args[Constants.TRANSFER_MODE].toString()
                )

                _transactionUiState.value =
                    transUiState.value.copy(
                        voiceState = null,
                        navigateToServiceTransfer = transferModel
                    )
                setArgs(null)

            }
        }

    }


    private fun setArgs(bundle: Bundle?) {
        savedStateHandle[Constants.EXTRAS] = bundle
    }


    fun onEvent(event: TransUiEvent) {
        when (event) {
            is TransUiEvent.GetExtras -> {
                event.extras?.let {
                    handleArguments(it)
                }
            }
            is TransUiEvent.GetCurrentBalance -> {
                viewModelScope.launch {
                    getCurrentBalance().onEach { currentBalance ->
                        _transactionUiState.value =
                            transUiState.value.copy(currentBalance = currentBalance.toString())
                    }.launchIn(viewModelScope)
                }

            }
            is TransUiEvent.GetTransactions -> {
                getTransactions().onEach { transactions ->
                    _transactionUiState.value =
                        transUiState.value.copy(transactions = transactions)
                }.launchIn(viewModelScope)

            }
            else -> Unit
        }
    }

    sealed class TransUiEvent {
        data class GetExtras(val extras: Bundle?) : TransUiEvent()
        object GetCurrentBalance : TransUiEvent()
        object GetTransactions : TransUiEvent()

    }

}

