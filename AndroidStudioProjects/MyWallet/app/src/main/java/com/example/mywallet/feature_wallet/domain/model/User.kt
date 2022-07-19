package com.example.mywallet.feature_wallet.domain.model

import android.os.Parcelable
import com.example.mywallet.feature_wallet.data.local.entity.UserEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    var userName: String? = "",
    val id: Int? = 0,
    val pin: Int? = 0,
    var phoneNumber: Int? = 0,
    var passport: String? = "",
    var dob: String? = "",
    var nationalId: String? = "",
    val password: String? = "",
    val isLoggedIn: Boolean = false,
    val filePath: String? = null
) : Parcelable {

    fun toUserEntity(): UserEntity {
        return UserEntity(
            userName = userName,
            id = id,
            pin = pin,
            phoneNumber = phoneNumber,
            passport = passport,
            dob = dob,
            nationalId = nationalId,
            password = password,
            isLoggedIn = isLoggedIn,
            filePath = filePath
        )
    }
}

