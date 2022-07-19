package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences

class SaveUserPin(
    private val userPreferences: UserPreferences
) {

     suspend  operator fun  invoke(pin: Int) {
        userPreferences.saveUserPin(pin)
    }

}