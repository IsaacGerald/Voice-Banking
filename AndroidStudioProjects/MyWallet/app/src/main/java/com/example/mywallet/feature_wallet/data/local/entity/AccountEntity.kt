package com.example.mywallet.feature_wallet.data.local.entity

data class AccountEntity(
    val id: Int = 0,
    val nationalId: Int = 0,
    val passport: Int = 0,
    val type: String? = null,
    val accountNumber: Int? = 0,
    val phoneNumber: Int = 0

)
