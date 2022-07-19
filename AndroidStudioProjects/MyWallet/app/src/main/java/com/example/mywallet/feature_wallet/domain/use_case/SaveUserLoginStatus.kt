package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences

class SaveUserLoginStatus(
    private val userPreferences: UserPreferences
) {

    suspend operator fun invoke(isLoggedIn: Boolean){
        userPreferences.isLoggedIn(isLoggedIn)
    }
}