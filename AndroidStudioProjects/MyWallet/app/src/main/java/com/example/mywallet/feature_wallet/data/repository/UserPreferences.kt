package com.example.mywallet.feature_wallet.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.mywallet.feature_wallet.domain.model.Transfer
import com.example.mywallet.feature_wallet.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


class UserPreferences(
    private val preferences: DataStore<Preferences>
) {

    private val TAG = UserPreferences::class.java.simpleName

    val getUserPin: Flow<Int> = preferences.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val pin = preferences[PreferenceKeys.PIN] ?: 0
            pin
        }


    val getUserPreferences: Flow<User?> = preferences.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val username = preferences[PreferenceKeys.USER_NAME] ?: ""
            val id = preferences[PreferenceKeys.ID] ?: -1
            val password = preferences[PreferenceKeys.PASSWORD] ?: ""
            val phoneNumber = preferences[PreferenceKeys.PHONE_NUMBER] ?: -1
            val isLoggedIn = preferences[PreferenceKeys.IS_LOGGED_IN] ?: false
            val passport = preferences[PreferenceKeys.PASSPORT] ?: ""
            val nationalId = preferences[PreferenceKeys.N_ID] ?: ""
            val dob = preferences[PreferenceKeys.DOB] ?: ""
            val pin = preferences[PreferenceKeys.PIN] ?: 0


            User(
                id = id,
                userName = username,
                phoneNumber = phoneNumber,
                password = password,
                isLoggedIn = isLoggedIn!!,
                dob = dob,
                passport = passport,
                nationalId = nationalId,
                pin = pin
            )

        }

    suspend fun saveUserPin(pin: Int) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.PIN] = pin
        }
    }

    suspend fun saveUser(
        username: String?,
        passport: String?,
        phoneNumber: Int?,
        nationalId: String?,
        pin: Int?,
        isLoggedIn: Boolean
    ) {
        preferences.edit { preferences ->
            Log.d(TAG, "saving user...: ")
            preferences[PreferenceKeys.USER_NAME] = username ?: ""
            preferences[PreferenceKeys.PASSPORT] = passport ?: ""
            preferences[PreferenceKeys.N_ID] = nationalId ?: ""
            preferences[PreferenceKeys.PIN] = pin ?: 0
            preferences[PreferenceKeys.PHONE_NUMBER] = phoneNumber ?: 0
            preferences[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun isLoggedIn(isLoggedIn: Boolean) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    fun getUserLogin(): Flow<Boolean> = preferences.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isLoggedIn = preferences[PreferenceKeys.IS_LOGGED_IN]
            isLoggedIn!!
        }


    suspend fun getIsUserValid(): Flow<Boolean> = preferences.data
        .map { preferences ->
            val isValid = preferences[PreferenceKeys.IS_VALID_USER] ?: false
            isValid
        }


    suspend fun getCurrentBalance(): Flow<Int> = preferences.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val amount = preferences[PreferenceKeys.CURRENT_BALANCE] ?: 230000

            amount
        }

    suspend fun saveCurrentBalance(amount: Int) {
        preferences.edit { preferences ->
            Log.d(TAG, "saveCurrentBalance: Saving balance")
            preferences[PreferenceKeys.CURRENT_BALANCE] = amount
        }
    }

    suspend fun updateLoggInstate(isLoggedIn: Boolean) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun updateValidUser(isValid: Boolean) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.IS_VALID_USER] = isValid
        }
    }

    suspend fun getTransfers(): Flow<Transfer> = preferences.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val originName = preferences[PreferenceKeys.ORIGIN_NAME] ?: ""
            val destinationName = preferences[PreferenceKeys.DESTINATION_NAME] ?: ""
            val value = preferences[PreferenceKeys.VALUE] ?: ""
            val currency = preferences[PreferenceKeys.CURRENCY] ?: ""
            val originProviderName = preferences[PreferenceKeys.ORIGIN_PROVIDER_NAME] ?: ""
            val originDestinationName = preferences[PreferenceKeys.ORIGIN_DEST_NAME] ?: ""
            val transferMode = preferences[PreferenceKeys.TRANSFER_MODE] ?: ""

            Transfer(
                originName,
                destinationName,
                value,
                currency,
                originProviderName,
                originDestinationName,
                transferMode
            )

        }

    fun getEnrollmentVoice(): Flow<String> = preferences.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map {preferences ->
            val filePath = preferences[PreferenceKeys.ENR_VOICE]
            filePath!!
        }

    suspend fun setIsNewUser(isNewUser: Boolean){
        preferences.edit { preferences ->
            preferences[PreferenceKeys.IS_NEW_USER] = isNewUser
        }
    }

    fun getIsNewUser(): Flow<Boolean> = preferences.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map {preferences ->
            val isNewUser = preferences[PreferenceKeys.IS_NEW_USER] ?: true
            isNewUser
        }




    suspend fun saveEnrolmentVoice(filePath: String){
        preferences.edit { preferences ->
            preferences[PreferenceKeys.ENR_VOICE] = filePath
        }
    }

    suspend fun clearTransfers() {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.ORIGIN_NAME] = ""
            preferences[PreferenceKeys.DESTINATION_NAME] = ""
            preferences[PreferenceKeys.VALUE] = ""
            preferences[PreferenceKeys.CURRENCY] = ""
            preferences[PreferenceKeys.ORIGIN_PROVIDER_NAME] = ""
            preferences[PreferenceKeys.ORIGIN_DEST_NAME] = ""
        }
    }


    suspend fun saveTransfer(
        originName: String?,
        destinationName: String?,
        value: String?,
        currency: String?,
        originProviderName: String?,
        originDestinationName: String?,
        transfer: String,
        isLoggedIn: Boolean = false
    ) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.ORIGIN_NAME] = originName ?: ""
            preferences[PreferenceKeys.DESTINATION_NAME] = destinationName ?: ""
            preferences[PreferenceKeys.VALUE] = value ?: ""
            preferences[PreferenceKeys.CURRENCY] = currency ?: ""
            preferences[PreferenceKeys.ORIGIN_PROVIDER_NAME] = originProviderName ?: ""
            preferences[PreferenceKeys.ORIGIN_DEST_NAME] = originDestinationName ?: ""
            preferences[PreferenceKeys.TRANSFER_MODE] = transfer
            preferences[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

}

private object PreferenceKeys {
    val USER_NAME = stringPreferencesKey("username");
    val ID = intPreferencesKey("id")
    val PASSWORD = stringPreferencesKey("password")
    val PHONE_NUMBER = intPreferencesKey("phone_number")
    val IS_LOGGED_IN = booleanPreferencesKey("isLoggedIn")
    val CURRENT_BALANCE = intPreferencesKey("currentBalance")
    val IS_VALID_USER = booleanPreferencesKey("isValidUser")
    val ORIGIN_NAME = stringPreferencesKey("originName")
    val DESTINATION_NAME = stringPreferencesKey("destinationName")
    val VALUE = stringPreferencesKey("value")
    val CURRENCY = stringPreferencesKey("currency")
    val ORIGIN_PROVIDER_NAME = stringPreferencesKey("originProvideName")
    val ORIGIN_DEST_NAME = stringPreferencesKey("originDestinationName")
    val TRANSFER_MODE = stringPreferencesKey("transferMode")
    val PASSPORT = stringPreferencesKey("passport")
    val N_ID = stringPreferencesKey("userNationalId")
    val PIN = intPreferencesKey("pin")
    val DOB = stringPreferencesKey("dob")
    val ENR_VOICE = stringPreferencesKey("enrollmentVoice")
    val IS_NEW_USER = booleanPreferencesKey("isNewUser")

}