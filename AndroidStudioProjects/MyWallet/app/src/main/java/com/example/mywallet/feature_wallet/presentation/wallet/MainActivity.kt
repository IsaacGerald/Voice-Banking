package com.example.mywallet.feature_wallet.presentation.wallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mywallet.R
import com.example.mywallet.core.util.VoiceInteraction
import com.example.mywallet.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName
    lateinit var binding: ActivityMainBinding
    lateinit var voiceInteraction: VoiceInteraction
    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        val extras = intent.extras


        if (extras != null) {
            navController.setGraph(R.navigation.nav_main, intent.extras)

        } else {
            navController.setGraph(R.navigation.nav_main)
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.transactionFragment,
                R.id.serviceTransferFragment,
                R.id.billsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)


    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        intent?.let {
//            it.handleIntent()
//        }
    }


    fun initVoiceInteraction(voiceInteraction: VoiceInteraction) {
        this.voiceInteraction = voiceInteraction
    }


    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()

        voiceInteraction.isVoiceInteraction()
    }
}