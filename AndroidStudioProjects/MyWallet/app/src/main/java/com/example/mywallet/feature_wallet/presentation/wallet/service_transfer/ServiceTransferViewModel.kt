package com.example.mywallet.feature_wallet.presentation.wallet.service_transfer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.mywallet.core.util.Constants
import com.example.mywallet.feature_wallet.domain.model.Transfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServiceTransferViewModel(
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _serviceUiState = MutableStateFlow(ServiceTransferUiState())
    private val transfers = savedStateHandle.getLiveData<Transfer>(Constants.TRANSFER_EXTRA)

    val serviceUiState: StateFlow<ServiceTransferUiState> = _serviceUiState


    fun setArgs(transferModel: Transfer) {
        savedStateHandle[Constants.TRANSFER_EXTRA] = transferModel
    }

    private fun handleTransfers(transfers: Transfer) {
        val path = getPath(transfers.transferMode.toString()) ?: return

        when (path) {
            Constants.RECEIVE_MONEY -> {
                onEvent(ServiceTransferUiEvent.ValidateReceiveTransfer)
            }
            Constants.SEND_MONEY -> {
                onEvent(ServiceTransferUiEvent.ValidateSendTransfer)
            }
        }


    }

    private fun validateReceiveTransfer() {
        if (!isValidReceiveAmount() || !isValidSource()) {
            return
        } else {

            _serviceUiState.value =
                serviceUiState.value.copy(
                    amount = Integer.parseInt(transfers.value?.value!!),
                    source = transfers.value?.originName,
                    voiceState = STVoiceState(confirmReceiveTransfer = true)
                )

        }
    }


    private fun getPath(transferMode: String): String? {
        val uri = Uri.parse(transferMode)
        return uri.path
    }

    private fun isValidReceiveAmount(): Boolean {
        val amount = transfers.value?.value
        return if (amount == null) {
            false
        } else {
            _serviceUiState.value =
                serviceUiState.value.copy(voiceState = STVoiceState(validateReceiveAmount = true))
            true
        }


    }

    private fun isValidSource(): Boolean {
        val source = transfers.value?.originName
        return if (source == null) {
            false
        } else {
            _serviceUiState.value =
                serviceUiState.value.copy(voiceState = STVoiceState(validateSourceOfTransfer = true))
            true
        }

    }


    fun onEvent(event: ServiceTransferUiEvent) {
        when (event) {
            is ServiceTransferUiEvent.GetExtras -> {
                handleTransfers(event.transferModel)
            }
            is ServiceTransferUiEvent.ValidateReceiveTransfer -> {
                validateReceiveTransfer()
            }
            is ServiceTransferUiEvent.ValidateSendTransfer -> {
                validateSendTransfer()
            }
        }
    }

    private fun validateSendTransfer() {

    }

    sealed class ServiceTransferUiEvent {
        data class GetExtras(val transferModel: Transfer) : ServiceTransferUiEvent()
        object ValidateReceiveTransfer : ServiceTransferUiEvent()
        object ValidateSendTransfer : ServiceTransferUiEvent()
    }

}