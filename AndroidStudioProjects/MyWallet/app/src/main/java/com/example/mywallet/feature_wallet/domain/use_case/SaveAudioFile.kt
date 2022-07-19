package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences

class SaveAudioFile(
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(filePath: String) {
        userPreferences.saveEnrolmentVoice(filePath)
    }
}