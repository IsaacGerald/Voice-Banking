package com.example.mywallet.feature_wallet.presentation.auth.onboarding.face

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialValidation
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserOnBoardingStatus

class FaceValidationViewModelFactory(
    owner: SavedStateRegistryOwner,
    private var getFacialValidation: GetFacialValidation,
    private val saveUserOnBoardingStatus: SaveUserOnBoardingStatus,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = FaceValidationViewModel(getFacialValidation, saveUserOnBoardingStatus, handle) as T
}