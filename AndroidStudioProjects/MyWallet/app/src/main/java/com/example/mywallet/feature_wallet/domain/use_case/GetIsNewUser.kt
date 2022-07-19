package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetIsNewUser(
    private val userPreferences: UserPreferences
) {

    operator fun invoke(): Flow<Boolean> = flow {
        userPreferences.getIsNewUser()
    }

}