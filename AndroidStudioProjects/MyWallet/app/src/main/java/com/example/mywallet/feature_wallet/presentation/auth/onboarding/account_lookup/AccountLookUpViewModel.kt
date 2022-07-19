package com.example.mywallet.feature_wallet.presentation.auth.onboarding.account_lookup

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.mywallet.core.util.isInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountLookUpViewModel : ViewModel() {
    private val TAG = AccountLookUpViewModel::class.java.simpleName
    private var _accountLookUpUiState = MutableStateFlow(AccountLookUpUIState())

    val accountLookUpUIState: StateFlow<AccountLookUpUIState> = _accountLookUpUiState

    private var _nationalId: String? = null
    private var _passport: String? = null
    private var _accountNumber: String? = null
    private var _phoneNumber: String? = null


    fun onEvent(event: AccountLookUpEvent) {
        when (event) {
            is AccountLookUpEvent.GetScreenDescription -> {
                _accountLookUpUiState.value =
                    accountLookUpUIState.value.copy(voiceState = AccountLkUpVoiceState(welcomePrompt = true))
            }
            is AccountLookUpEvent.GetUserDetail -> {
                handleDetailState(event.state)
            }

            is AccountLookUpEvent.SaveData -> {
                saveUserData(event.data)
            }
            is AccountLookUpEvent.GetDataState -> {
                _accountLookUpUiState.value =
                    accountLookUpUIState.value.copy(voiceState = null, dataState = event.dataState)
            }

            is AccountLookUpEvent.ActivateMobileBanking -> {
                updateVoiceState(AccountLkUpVoiceState(activateMobileBanking = true))
            }

        }
    }

    private fun saveUserData(data: String?) {
        val dataState = accountLookUpUIState.value.dataState
        if (dataState != null) {
            when {
                dataState.isAccount -> {
                    if (data != null) {
                        if (isInteger(data)) {
                            Log.d(TAG, "saveUserData: ")
                            _accountNumber = data
                            updateVoiceState(AccountLkUpVoiceState(agreeToTermsAndConditions = true))
                        } else {

                            updateVoiceState(AccountLkUpVoiceState(invalidAccountNumber = true))

                        }
                    }
                }
                dataState.isPhoneNumber -> {
                    if (data != null) {
                        if (isInteger(data)) {
                            _phoneNumber = data
                            updateVoiceState(AccountLkUpVoiceState(agreeToTermsAndConditions = true))
                        } else {
                            updateVoiceState(
                                AccountLkUpVoiceState(invalidPhoneNumber = true)
                            )
                        }
                    }
                }
                dataState.isNationalId -> {
                    if (data != null) {
                        if (isInteger(data)) {
                            _nationalId = data
                            updateVoiceState(AccountLkUpVoiceState(agreeToTermsAndConditions = true))

                        } else {

                            updateVoiceState(AccountLkUpVoiceState(invalidId = true))

                        }
                    }
                }
                dataState.isPassport -> {
                    if (data != null) {
                        if (isInteger(data)) {
                            _passport = data
                            updateVoiceState(AccountLkUpVoiceState(agreeToTermsAndConditions = true))
                        } else {
                            updateVoiceState(AccountLkUpVoiceState(invalidPassport = true))
                        }
                    }
                }
            }
        }
    }

    private fun updateVoiceState(voiceState: AccountLkUpVoiceState) {
        _accountLookUpUiState.value = accountLookUpUIState.value.copy(
            voiceState = voiceState
        )
    }

    private fun handleDetailState(state: String) {

    }


    sealed class AccountLookUpEvent {
        object GetScreenDescription : AccountLookUpEvent()
        object ActivateMobileBanking : AccountLookUpEvent()
        data class GetUserDetail(val state: String) : AccountLookUpEvent()
        data class SaveData(val data: String?) : AccountLookUpEvent()
        data class GetDataState(val dataState: DataState) : AccountLookUpEvent()
    }


}