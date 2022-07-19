package com.example.mywallet.feature_wallet.presentation.ui

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository

class TransactionViewModelFactory(
     owner: SavedStateRegistryOwner,
     private val repository: TransactionRepository,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = TransactionViewModel(handle, repository ) as T
}