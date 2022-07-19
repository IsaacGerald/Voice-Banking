package com.example.mywallet.feature_wallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mywallet.feature_wallet.domain.model.Transaction

@Entity(tableName = "transaction_table")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionName: String,
    val amount: Int,
    val type: String,
    val date: String
){
    fun toTransaction(): Transaction{
        return Transaction(
            id = id,
            transactionName = transactionName,
            amount = amount,
            type = type,
            date = date
        )
    }
}
