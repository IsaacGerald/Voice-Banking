package com.example.mywallet.feature_wallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_details_table")
data class UserDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: Int = 0,
    val nationalId: Int = 0,
    val passport: Int = 0,
    val fullName: String? = null,
    val emailAddress: String? = null,
    val accounts: List<AccountEntity>? = null,
    val isMobileBankingActivated: Boolean = false
)