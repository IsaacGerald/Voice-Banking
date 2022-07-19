package com.example.mywallet.feature_wallet.domain.repository

import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.data.local.entity.TransactionEntity
import com.example.mywallet.feature_wallet.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface TransactionRepository {

    suspend fun getTransactions(): List<TransactionEntity>

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun clearTransactions()

    fun getFacialValidation(photoFile: File?): Flow<Resource<FaceModel>>

    fun getFaceLogin(photoFile: File?): Flow<Resource<FaceModel>>

    fun getVoiceValidation(audio: File?): Flow<Resource<List<VoiceModel>>>

    fun getVoiceLogin(enrollmentVoice: File?, candidateFile: File?): Flow<Resource<Prediction>>

    suspend fun getUsers(): List<User>

    suspend fun insertUser(user: User)




}