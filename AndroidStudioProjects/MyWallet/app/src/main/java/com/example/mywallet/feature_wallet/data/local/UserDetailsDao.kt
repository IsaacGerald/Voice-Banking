package com.example.mywallet.feature_wallet.data.local

import androidx.room.*
import com.example.mywallet.feature_wallet.data.local.entity.UserEntity

@Dao
interface UserDetailsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserEntity)

    @Update
    suspend fun updateUser(userEntity: UserEntity)

    @Query("SELECT * FROM user_details_table")
    suspend fun getUserDetails(): List<UserEntity>
}