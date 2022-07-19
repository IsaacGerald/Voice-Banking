package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.VoiceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class GetVoiceValidation(
    private val repository: TransactionRepository
) {

    operator fun invoke(enrollmentVoice: File): Flow<Resource<List<VoiceModel>>> {
           return repository.getVoiceValidation(enrollmentVoice)
    }
}