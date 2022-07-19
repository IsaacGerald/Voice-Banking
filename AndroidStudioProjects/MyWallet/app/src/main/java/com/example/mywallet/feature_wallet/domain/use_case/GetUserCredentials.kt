package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetUserCredentials(
    private val userPreferences: UserPreferences
) {

    operator fun invoke(): Flow<User?> = flow {
        userPreferences.getUserPreferences
    }

}