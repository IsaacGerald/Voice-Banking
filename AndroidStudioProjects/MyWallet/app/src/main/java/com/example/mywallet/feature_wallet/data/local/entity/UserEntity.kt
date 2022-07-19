package com.example.mywallet.feature_wallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mywallet.feature_wallet.domain.model.User

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = 0,
    var userName: String? = "",
    val pin: Int? = 0,
    var phoneNumber: Int? = 0,
    var passport: String? = "",
    var dob: String? = "",
    var nationalId: String? = "",
    val password: String? = "",
    val isLoggedIn: Boolean = false,
    val filePath: String? = null,
) {
    fun toUser(): User {
        return User(
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