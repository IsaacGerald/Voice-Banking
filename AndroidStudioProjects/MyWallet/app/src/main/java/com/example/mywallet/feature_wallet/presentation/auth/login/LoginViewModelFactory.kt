package com.example.mywallet.feature_wallet.presentation.auth.login

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.*

class LoginViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val getVoiceLogin: GetVoiceLogin,
    private val getAudioFile: GetAudioFile,
    private val getUsers: GetUsers,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = LoginViewModel(getVoiceLogin, getAudioFile, getUsers, handle ) as T
}