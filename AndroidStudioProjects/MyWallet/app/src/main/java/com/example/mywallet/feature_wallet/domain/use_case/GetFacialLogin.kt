package com.example.mywallet.feature_wallet.domain.use_case

import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class GetFacialLogin(
    private var repository: TransactionRepository
) {

    operator fun invoke(photoFile: File?): Flow<Resource<FaceModel>> {

        return repository.getFaceLogin(photoFile)
    }

}