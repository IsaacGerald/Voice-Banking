package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.domain.model.Transaction
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetTransactions(
 private val  repository: TransactionRepository
) {

    operator fun invoke(): Flow<List<Transaction>> = flow {
       emit(repository.getTransactions().map { it.toTransaction() })
    }
}