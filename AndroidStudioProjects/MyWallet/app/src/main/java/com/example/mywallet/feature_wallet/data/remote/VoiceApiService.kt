package com.example.mywallet.feature_wallet.data.remote

import com.example.mywallet.feature_wallet.data.remote.dto.VoiceDto
import com.example.mywallet.feature_wallet.domain.model.Prediction
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


val voiceRetrofit: Retrofit = Retrofit.Builder()
    .baseUrl(VoiceApiService.BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()


interface VoiceApiService {

    @Multipart
    @POST("voiceAuthenticationPrediction")
    suspend fun getAudioRegistration(
        @Part file: MultipartBody.Part,
    ): List<VoiceDto>

    @Multipart
    @POST("voiceAuthenticationPrediction")
    suspend fun getAudioLogin(
        @Part enrollmentVoice: MultipartBody.Part,
        @Part candidateVoice: MultipartBody.Part,
    ): Prediction

    companion object {
        const val BASE_URL = "https://e2c5-41-90-184-38.eu.ngrok.io"
    }

}

object VoiceValidationService {
    val voiceValidationApi: VoiceApiService by lazy {
        voiceRetrofit.create(VoiceApiService::class.java)
    }
}