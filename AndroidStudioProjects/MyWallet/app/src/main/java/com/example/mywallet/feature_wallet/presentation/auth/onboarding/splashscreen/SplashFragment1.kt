package com.example.mywallet.feature_wallet.presentation.auth.onboarding.splashscreen

import android.app.VoiceInteractor
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.databinding.FragmentSplash1Binding
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import kotlinx.coroutines.flow.collectLatest

class SplashFragment1 : Fragment(R.layout.fragment_splash1), LoginVoiceInteraction {
    lateinit var binding: FragmentSplash1Binding
    lateinit var voiceState: Splash1VoiceState
    lateinit var viewModel: SplashFragment1ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSplash1Binding.bind(view)
        viewModel = SplashFragment1ViewModel()

        viewModel.onEvent(SplashFragment1ViewModel.Splash1Event.WelcomeUserPrompt)

        lifecycleScope.launchWhenStarted {
            subscribeUiEvent()
        }


    }

    private suspend fun subscribeUiEvent() {
        viewModel.splashUiState.collectLatest { state ->

            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

        }
    }

    override fun isVoiceInteractionStarted() {
        when {
            voiceState.welcomePrompt -> {
                Log.i(Companion.TAG, "isVoiceInteractionStarted: ")
                welcomePrompt()
            }
        }
    }

    private fun welcomePrompt() {
        val prompt =
            VoiceInteractor.Prompt(
                "Hello there, " +
                        "welcome to Eclectics Bank, we are happy to have you here." +
                        "  do you have an eclectics account?"
            )

        val option1 = VoiceInteractor.PickOptionRequest.Option("yes", 1);
        option1.addSynonym("yes, i have an eclectics account")
        option1.addSynonym("Yes, i have an account")
        option1.addSynonym("I have an account")
        option1.addSynonym(" I have an eclectics account")
        option1.addSynonym(" Yes, I have")
        option1.addSynonym(" I have")
        option1.addSynonym(" i have")

        val option2 = VoiceInteractor.PickOptionRequest.Option("no", 1);
        option2.addSynonym("No, i don' t have an eclectics account")
        option2.addSynonym("No, i don' t have an account")
        option2.addSynonym("No, i don't have")
        option2.addSynonym(" I don't have")


        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    activity?.stopLocalVoiceInteraction()
                    when (selections[0].index) {
                        0 -> {
                            navigateToAccountLookUp()
                        }
                        1 -> {
                            navigateToAccountSetUp()
                        }
                    }

                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        activity?.stopLocalVoiceInteraction()
    }

    private fun navigateToAccountSetUp() {

    }

    private fun navigateToAccountLookUp() {
        val action = SplashFragment1Directions.actionSplashFragment1ToAccountLookupFragment()
        findNavController().navigate(action)
    }

    companion object {
        private const val TAG = "SplashFragment1"
    }
}