package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import kotlinx.coroutines.flow.Flow

class GetAudioFile(
    private val userPreferences: UserPreferences
) {
    operator fun invoke(): Flow<String> {
        return userPreferences.getEnrollmentVoice()
    }
}