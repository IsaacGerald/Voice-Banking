package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetUsers(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<User>> = flow {
        emit(repository.getUsers())
    }
}