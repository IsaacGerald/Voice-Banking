package com.example.mywallet.feature_wallet.presentation.auth.login

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.mywallet.R
import com.example.mywallet.databinding.ActivityLoginBinding
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.USER_PREFERENCES_NAME

val Context.dataStore by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    lateinit var navController: NavController
    private var voiceInteraction: VoiceInteraction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost_fragment_login) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.welcomeLoginFragment,
                R.id.loginFragment2,
                R.id.faceLoginFragment,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }


    fun initVoiceInteraction(voiceInteraction: VoiceInteraction) {
        this.voiceInteraction = voiceInteraction
    }

    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()

        voiceInteraction?.isVoiceInteractionStarted()
    }


    override fun onDestroy() {
        stopLocalVoiceInteraction()
        super.onDestroy()
    }

}

interface VoiceInteraction {
    fun isVoiceInteractionStarted()
}