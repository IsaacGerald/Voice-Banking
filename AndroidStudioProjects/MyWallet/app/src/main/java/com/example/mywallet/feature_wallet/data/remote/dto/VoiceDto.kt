package com.example.mywallet.feature_wallet.data.remote.dto

import com.example.mywallet.feature_wallet.domain.model.VoiceModel

data class VoiceDto(
    val loc: List<String>,
    val msg: String,
    val type: String
) {

    fun toModel(): VoiceModel {
        return VoiceModel(
            loc = loc,
            msg = msg,
            type = type
        )
    }
}