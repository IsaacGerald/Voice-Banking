package com.example.mywallet.feature_wallet.presentation.auth.onboarding

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.mywallet.R

const val USER_PREFERENCES_NAME = "user_preferences"

val Context.dataStore by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class OnBoardingActivity : AppCompatActivity() {
    private val TAG = OnBoardingActivity::class.java.simpleName
    lateinit var loginVoiceInteraction: LoginVoiceInteraction
    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost_fragment_onboarding) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.MBActivationFragment,
                R.id.splashFragment1,
                R.id.accountLookupFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)


    }

    fun initLocalVoiceInteraction(loginVoiceInteraction: LoginVoiceInteraction) {
        this.loginVoiceInteraction = loginVoiceInteraction
    }


    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()
        loginVoiceInteraction.isVoiceInteractionStarted()

    }

    override fun onDestroy() {
        stopLocalVoiceInteraction()
        super.onDestroy()
    }
}

interface LoginVoiceInteraction {
    fun isVoiceInteractionStarted()
}