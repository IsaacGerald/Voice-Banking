package com.example.mywallet.feature_wallet.presentation.auth.login.welcomescreen

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.*

class WelcomeLoginViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val getIsNewUser: GetIsNewUser,
    private val getVoiceLogin: GetVoiceLogin,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = WelcomeLoginViewModel(getIsNewUser, getVoiceLogin, handle ) as T
}