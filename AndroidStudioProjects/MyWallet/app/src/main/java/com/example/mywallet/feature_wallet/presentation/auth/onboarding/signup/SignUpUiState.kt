package com.example.mywallet.feature_wallet.presentation.auth.onboarding.signup

data class SignUpUiState(
    val voiceState: SignupVoiceState? = null,
    val credentialState: CredentialState? = null,
    val passportOrId: String? = null
)


data class SignupVoiceState(
    val isPassportOrId: Boolean = false,
    val isPhoneNumber: Boolean = false,
    val isUsername: Boolean = false,
    val enterPassportOrId: Boolean = false,
    val confirmAccount: Boolean = false,
    val createAccount: Boolean = false,
    val enableBiometrics: Boolean = false,
    val createAnotherAccount: Boolean = false,
    val passportIsInvalid: Boolean = false,
    val idIsInvalid: Boolean = false,
    val phoneNumberIsInvalid: Boolean = false
)

data class CredentialState(
    val isPhoneNumber: Boolean = false,
    val isUserName: Boolean = false,
    val isPassportOrId: Boolean = false
)

