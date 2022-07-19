package com.example.mywallet.feature_wallet.data.local

import androidx.room.*
import com.example.mywallet.feature_wallet.data.local.entity.TransactionEntity
import com.example.mywallet.feature_wallet.domain.model.Transaction

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transaction_table")
    suspend fun getTransactions():List<TransactionEntity>

     @Query("DELETE FROM transaction_table")
     suspend fun clearTransactions()



}