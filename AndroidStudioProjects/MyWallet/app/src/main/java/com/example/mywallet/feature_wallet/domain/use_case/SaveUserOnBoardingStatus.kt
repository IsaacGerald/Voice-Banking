package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences

class SaveUserOnBoardingStatus(
    private val userPreferences: UserPreferences
) {

    suspend operator fun invoke(isNewUser: Boolean){
        userPreferences.setIsNewUser(isNewUser)
    }
}