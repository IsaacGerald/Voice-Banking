package com.example.mywallet.feature_wallet.presentation.auth.onboarding.voice

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceRegistration
import com.example.mywallet.feature_wallet.domain.use_case.SaveAudioFile
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser

class RecordAudioViewModelFactory(
    owner: SavedStateRegistryOwner,
    private var getVoiceRegistration: GetVoiceRegistration,
    private var  saveAudioFile: SaveAudioFile,
    private val saveUser: SaveUser,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = AudioViewModel(getVoiceRegistration, saveAudioFile, saveUser, handle) as T
}