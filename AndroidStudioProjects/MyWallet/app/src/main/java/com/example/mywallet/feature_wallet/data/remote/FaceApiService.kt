package com.example.mywallet.feature_wallet.data.remote

import com.example.mywallet.feature_wallet.data.remote.dto.FaceDto
import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


val gson = GsonBuilder()
    .setLenient()
    .create()
val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

val okHttpClient = OkHttpClient().newBuilder()
    .addInterceptor(interceptor)
    .connectTimeout(5, TimeUnit.MINUTES)
    .readTimeout(5, TimeUnit.MINUTES)
    .writeTimeout(5, TimeUnit.MINUTES)
    .build();

val retrofit = Retrofit.Builder()
    .baseUrl(FaceApiService.BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()


interface FaceApiService {

    @Multipart
    @POST("register")
    suspend fun getValidation(
        @Part file: MultipartBody.Part,
    ): Response<FaceDto>

    @Multipart
    @POST(".")
    suspend fun getFaceLogin(
        @Part file: MultipartBody.Part,
    ): Response<FaceDto>

    companion object {
        const val BASE_URL = "http://10.42.0.226:5000/"
    }

}


object FaceValidationService {
    val validationApi: FaceApiService by lazy {
        retrofit.create(FaceApiService::class.java)
    }
}