package com.example.mywallet.feature_wallet.presentation.auth.onboarding.signup

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserCredentials

class SignUpViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val saveUserCredentials: SaveUserCredentials,
    private val saveUser: SaveUser,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = SignUpViewModel(saveUserCredentials, saveUser, handle ) as T
}