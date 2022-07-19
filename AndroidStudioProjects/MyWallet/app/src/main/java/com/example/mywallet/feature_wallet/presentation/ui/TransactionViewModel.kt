package com.example.mywallet.feature_wallet.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.example.mywallet.core.util.Resource
import com.example.mywallet.core.util.TRANSFER_KEY
import com.example.mywallet.feature_wallet.domain.model.Transaction
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class TransactionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
) : ViewModel() {
    private val TAG = TransactionViewModel::class.java.simpleName
    val transfer = savedStateHandle.getLiveData<Bundle>(TRANSFER_KEY)
    private val _isValidUser = MutableLiveData<Boolean>()
    val isValidUser: LiveData<Boolean>
        get() = _isValidUser


    private val _showBalance = MutableLiveData<Boolean>(false)
    val showBalance: LiveData<Boolean>
        get() = _showBalance

    private val _validation = MutableLiveData<Resource<FaceModel>>()
    val validation: LiveData<Resource<FaceModel>>
        get() = _validation


    private val _transaction = MutableLiveData<List<Transaction>>(null)
    private val _mTransferOriginName = MutableLiveData<String>(null)
    val mTransferOriginName: LiveData<String>
        get() = _mTransferOriginName

    private val _mTransferDestinationName = MutableLiveData<String>(null)
    val mTransferDestinationName: LiveData<String>
        get() = _mTransferDestinationName

    private val _value = MutableLiveData<Long>(null)
    val value: LiveData<Long>
        get() = _value

    val transaction: LiveData<List<Transaction>?>
        get() = _transaction
    val _currentBalance = MutableLiveData<Int>()

    val currentBalance: LiveData<Int>
        get() = _currentBalance

    init {
        getTransaction()
    }


    private fun getTransaction() {
        viewModelScope.launch {
            val transactions = repository.getTransactions()
            _transaction.postValue(transactions.map { it.toTransaction() })
        }
    }

    fun insertTransaction(transaction: Transaction?) {
        viewModelScope.launch {
            if (transaction != null) {
                repository.insertTransaction(transaction)
            }
        }

    }


    fun setCurrentValue(value: Int) {
        _currentBalance.postValue(value)
    }

    fun setValue(value: Long) {
        _value.postValue(value)
    }

    fun setDestinationName(destinationName: String) {
        _mTransferDestinationName.postValue(destinationName)
    }

    fun setOriginName(originName: String) {
        _mTransferOriginName.postValue(originName)
    }

    fun clearTransactions() {
        viewModelScope.launch {
            repository.clearTransactions()
        }
    }

    fun showBalance(showBalance: Boolean) {
        _showBalance.postValue(showBalance)
    }

    fun getValidation(file: String, photoFile: File?): Flow<Resource<FaceModel>> {
        Log.d(TAG, "getValidation: viewmodel getvalidation...")
        return repository.getFacialValidation(photoFile)
    }

    fun setTransferData(extras: Bundle?) {
        savedStateHandle[TRANSFER_KEY] = extras
    }

    fun clearTransferData() {
        //savedStateHandle[TRANSFER_KEY] = null
    }

    fun setIsUserVerified(isVerified: Boolean) {
        _isValidUser.value = isVerified
    }
}