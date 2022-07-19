package com.example.mywallet.feature_wallet.presentation.auth.onboarding.pin

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserPin

class PinViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val saveUserPin: SaveUserPin,
    private val saveUser: SaveUser,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = PinViewModel(saveUserPin, saveUser, handle) as T
}