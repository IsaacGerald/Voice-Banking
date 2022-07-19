package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.User

class SaveUserCredentials(
    private val userPreferences: UserPreferences
) {

    suspend operator fun invoke(user: User) {
        userPreferences.saveUser(
            username = user.userName,
            passport = user.passport,
            phoneNumber = user.phoneNumber,
            nationalId = user.nationalId,
            pin  = user.pin,
            isLoggedIn = false
        )
    }

}