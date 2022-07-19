package com.example.mywallet.feature_wallet.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_table")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionName: String,
    val amount: Int,
    val type: String,
    val date: String
)
