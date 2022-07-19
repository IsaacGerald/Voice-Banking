package com.example.mywallet.feature_wallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mywallet.feature_wallet.data.local.TransactionDao
import com.example.mywallet.feature_wallet.data.local.UserDao
import com.example.mywallet.feature_wallet.data.local.entity.UserEntity
import com.example.mywallet.feature_wallet.domain.model.Transaction

@Database(entities = [Transaction::class, UserEntity::class], version = 4, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao


    companion object {
        private var INSTANCE: TransactionDatabase? = null

        fun getDatabase(context: Context): TransactionDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java, "Trasnsactiondb"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }

        }
    }


}