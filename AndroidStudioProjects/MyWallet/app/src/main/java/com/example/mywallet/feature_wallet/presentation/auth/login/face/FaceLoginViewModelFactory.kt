package com.example.mywallet.feature_wallet.presentation.auth.login.face

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialLogin
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserLoginStatus

class FaceLoginViewModelFactory(
    private var getFacialLogin: GetFacialLogin,
    private var saveUserLoginStatus: SaveUserLoginStatus,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = FaceLoginViewModel(getFacialLogin,saveUserLoginStatus, handle ) as T
}