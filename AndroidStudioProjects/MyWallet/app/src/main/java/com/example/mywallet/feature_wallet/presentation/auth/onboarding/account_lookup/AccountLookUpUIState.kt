package com.example.mywallet.feature_wallet.presentation.auth.onboarding.account_lookup

data class AccountLookUpUIState(
    val voiceState: AccountLkUpVoiceState? = null,
    val dataState: DataState? = null,
    val AgreementChecked: Boolean = false,
)

data class AccountLkUpVoiceState(
    val welcomePrompt: Boolean = false,
    val invalidPhoneNumber: Boolean = false,
    val invalidAccountNumber: Boolean = false,
    val invalidId: Boolean = false,
    val invalidPassport: Boolean = false,
    val activateMobileBanking: Boolean = false,
    val agreeToTermsAndConditions: Boolean = false
)

data class DataState(
    val isPhoneNumber: Boolean = false,
    val isNationalId: Boolean = false,
    val isPassport: Boolean = false,
    val isAccount: Boolean = false
)