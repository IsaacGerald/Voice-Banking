package com.example.mywallet.feature_wallet.data.remote.dto

import com.example.mywallet.feature_wallet.domain.model.FaceModel

data class FaceDto(
    val condition: String,
    val status: Boolean
){
    fun toModel(): FaceModel {
        return FaceModel(
            condition = condition,
            status = status
        )
    }
}