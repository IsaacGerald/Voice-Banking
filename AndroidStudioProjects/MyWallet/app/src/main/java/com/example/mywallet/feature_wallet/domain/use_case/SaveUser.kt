package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository

class SaveUser(
    private var repository: TransactionRepository
) {
    suspend operator fun invoke(user: User) {
        repository.insertUser(user)
    }

}