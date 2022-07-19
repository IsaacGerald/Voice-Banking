package com.example.mywallet.feature_wallet.presentation.wallet.transaction

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.mywallet.feature_wallet.domain.use_case.GetCurrentBalance
import com.example.mywallet.feature_wallet.domain.use_case.GetTransactions

class TransactionsViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val getTransactions: GetTransactions,
    private val getCurrentBalance: GetCurrentBalance,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = TransactionsViewModel(handle, getTransactions, getCurrentBalance) as T
}