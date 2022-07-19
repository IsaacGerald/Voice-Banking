package com.example.mywallet.feature_wallet.domain.model

data class UserDetail(
    val phoneNumber: Int = 0,
    val nationalId: Int = 0,
    val fullName: String? = null,
    val passport: Int = 0,
    val emailAddress: String? = null,
    val isMobileBankingActivated: Boolean = false
)