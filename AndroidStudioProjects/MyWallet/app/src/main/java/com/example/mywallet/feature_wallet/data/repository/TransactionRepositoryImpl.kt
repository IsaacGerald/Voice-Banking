package com.example.mywallet.feature_wallet.data.repository

import android.util.Log
import com.example.mywallet.core.util.Resource
import com.example.mywallet.feature_wallet.data.local.TransactionDao
import com.example.mywallet.feature_wallet.data.local.UserDao
import com.example.mywallet.feature_wallet.data.local.entity.TransactionEntity
import com.example.mywallet.feature_wallet.data.remote.FaceApiService
import com.example.mywallet.feature_wallet.data.remote.VoiceApiService
import com.example.mywallet.feature_wallet.domain.model.*
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException


class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val userDao: UserDao,
    private val faceApi: FaceApiService,
    private val voiceApi: VoiceApiService,
) : TransactionRepository {
    private val TAG = TransactionRepositoryImpl::class.java.simpleName
    override suspend fun getTransactions(): List<TransactionEntity> {
        return dao.getTransactions()
    }


    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    override suspend fun clearTransactions() {
        dao.clearTransactions()
    }

    override fun getFacialValidation(
        photoFile: File?
    ): Flow<Resource<FaceModel>> = flow {

        emit(Resource.Loading(null))

        try {

            Log.d(TAG, "getValidation: repos -> getting data, photofileName -> ${photoFile?.name}")

            val requestBody = photoFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestBody)
            val fullName: RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "1234")

            val response = faceApi.getValidation(filePart)
            if (response.isSuccessful) {
                emit(Resource.Success(data = response.body()?.toModel()))
            } else {
                emit(Resource.Error(message = response.message()))
            }

        } catch (e: IOException) {
            Log.d(TAG, "getValidation: IOException -> ${e.message}")
            emit(
                Resource.Error(
                    message = "Couldn't reach server, check your internet connection : ${e.message}",
                    data = null
                )
            )
        } catch (e: HttpException) {
            //In case of errors occurred
            Log.d(TAG, "getValidation: HttpException -> ${e.response()}")
            emit(
                Resource.Error(
                    message = "Oops, something went wrong: ${e.message}", null
                )
            )
        }


    }

    override fun getFaceLogin(photoFile: File?): Flow<Resource<FaceModel>> = flow {
        emit(Resource.Loading(null))

        try {

            Log.d(TAG, "getValidation: repos -> getting data, photofileName -> ${photoFile?.name}")

            val requestBody = photoFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestBody)
            val fullName: RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "1234")

            val response = faceApi.getFaceLogin(filePart)
            if (response.isSuccessful) {
                emit(Resource.Success(data = response.body()?.toModel()))
            } else {
                emit(Resource.Error(message = response.message()))
            }

        } catch (e: IOException) {
            Log.d(TAG, "getValidation: IOException -> ${e.message}")
            emit(
                Resource.Error(
                    message = "Couldn't reach server, check your internet connection",
                    data = null
                )
            )
        } catch (e: HttpException) {
            //In case of errors occurred
            Log.d(TAG, "getValidation: HttpException -> ${e.response()}")
            emit(
                Resource.Error(
                    message = "Oops, something went wrong", null
                )
            )
        }

    }

    override fun getVoiceValidation(audioFile: File?): Flow<Resource<List<VoiceModel>>> = flow {
        emit(Resource.Loading(null))

        try {

            Log.d(TAG, "getVoiceLogin: repos -> getting data, audio fileName -> ${audioFile?.name}")

            val requestBody = audioFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestBody)

            val result = voiceApi.getAudioRegistration(filePart)

            emit(Resource.Success(data = result.map { it.toModel() }))


        } catch (e: IOException) {
            Log.d(TAG, "getValidation: IOException -> ${e.message}")
            emit(
                Resource.Error(
                    message = "Couldn't reach server, check your internet connection : ${e.message}",
                    data = null
                )
            )
        } catch (e: HttpException) {
            //In case of errors occurred
            Log.d(TAG, "getValidation: HttpException -> ${e.response()}")
            emit(
                Resource.Error(
                    message = "Oops, something went wrong: ${e.message}", null
                )
            )
        }
    }


    override fun getVoiceLogin(
        enrollmentVoice: File?,
        candidateFile: File?
    ): Flow<Resource<Prediction>> = flow {

        emit(Resource.Loading(null))

        try {

            Log.d(
                TAG,
                "getVoiceLogin: repos -> getting data, audio fileName -> ${enrollmentVoice?.name}"
            )

            val requestBody1 =
                enrollmentVoice!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val enrollmentFilePart =
                MultipartBody.Part.createFormData(
                    "enrollmentVoice",
                    enrollmentVoice.name,
                    requestBody1
                )

            val requestBody2 =
                candidateFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val candidateFilePart =
                MultipartBody.Part.createFormData(
                    "candidateVoice",
                    enrollmentVoice.name,
                    requestBody2
                )


            val result = voiceApi.getAudioLogin(enrollmentFilePart, candidateFilePart)

            emit(Resource.Success(data = result))


        } catch (e: IOException) {
            Log.d(TAG, "getValidation: IOException -> ${e.message}")
            emit(
                Resource.Error(
                    message = "Couldn't reach server, check your internet connection : ${e.message}",
                    data = null
                )
            )
        } catch (e: HttpException) {
            //In case of errors occurred
            Log.d(TAG, "getValidation: HttpException -> ${e.response()}")
            emit(
                Resource.Error(
                    message = "Oops, something went wrong: ${e.message}", null
                )
            )
        }
    }

    override suspend fun getUsers(): List<User> {
        return userDao.getUser().map { it.toUser() }
    }

    override suspend fun insertUser(user: User) {
        userDao.insertUser(user.toUserEntity())
    }


}