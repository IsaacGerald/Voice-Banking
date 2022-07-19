package com.example.mywallet.feature_wallet.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transfer(
    val originName: String?,
    val destinationName: String?,
    val value: String?,
    val currency: String?,
    val originProviderName: String?,
    val originDestinationName: String?,
    val transferMode: String?
) : Parcelable