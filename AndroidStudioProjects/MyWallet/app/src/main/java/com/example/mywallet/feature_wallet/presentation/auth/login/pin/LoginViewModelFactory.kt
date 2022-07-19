package com.example.mywallet.feature_wallet.presentation.auth.login.pin

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.GetUserPin
import com.example.mywallet.feature_wallet.domain.use_case.GetUsers

class LoginPinViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val getUserPin: GetUserPin,
    private val getUsers: GetUsers,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = LoginPinViewModel(getUserPin, getUsers, handle ) as T
}