package com.example.mywallet.feature_wallet.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FaceModel(
    val condition: String = "Unknown user",
    val status: Boolean = false
) : Parcelable
