package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.Prediction
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class GetVoiceLogin(
    private val repository: TransactionRepository
) {

    operator fun invoke(enrollmentVoice: File,  candidateFile: File): Flow<Resource<Prediction>> {
           return repository.getVoiceLogin(enrollmentVoice, candidateFile)
    }
}