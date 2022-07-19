package com.example.mywallet.feature_wallet.presentation.auth.onboarding.activation

import android.app.VoiceInteractor
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.databinding.FragmentMBActivationBinding
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class MBActivationFragment : Fragment(R.layout.fragment_m_b_activation), LoginVoiceInteraction {
    private val TAG = MBActivationFragment::class.java.simpleName
    lateinit var binding: FragmentMBActivationBinding
    lateinit var viewModel: MBActivationViewModel
    lateinit var voiceState: ActivationVoiceState
    lateinit var dataState: DataState
    private var timer = 50
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMBActivationBinding.bind(view)
        viewModel = MBActivationViewModel()

        viewModel.onEvent(MBActivationViewModel.ActivationEvent.GetScreenDescription)


        lifecycleScope.launchWhenStarted {
            initSpeechRecognition()
            subscribeActivationUiState()
        }


    }

    private suspend fun subscribeActivationUiState() {
        viewModel.uiState.collect { state ->
            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(Bundle())
            }
            state.dataState?.let {
                dataState = it
            }

            if (state.verifyDevice) {
                navigateToOTPFragment()
            }
        }
    }


    private suspend fun startListening() {
        if (timer == 0) {
            timer = 50
        }
        withContext(Dispatchers.Main) {
            while (timer > 1) {
                timer--
                speechRecognizer.startListening(speechRecognizerIntent)
                delay(1000)
            }
        }

    }

    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.welcomePrompt -> {
                    welcomePrompt()
                }
                voiceState.promptPhoneNumber -> {
                    enterPhoneNumberPrompt()
                }
                voiceState.promptFullName -> {
                    promptFullName()
                }
                voiceState.promptEmailAddress -> {
                    promptEmailAddress()
                }
                voiceState.invalidEmailAddress -> {
                    promptInvalidEmailAddress()
                }
                voiceState.invalidPhoneNumber -> {
                    promptInvalidPhoneNumber()
                }
                voiceState.invalidFullName -> {
                    promptInvalidFullName()
                }
                voiceState.activateUserPrompt -> {
                    activateUserPrompt()
                }
            }
        }
    }

    private fun activateUserPrompt() {
        val prompt =
            VoiceInteractor.Prompt("We are activating your account for Mobile banking,")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.startLocalVoiceInteraction(Bundle())
                navigateToOTPFragment()
            }
        })
    }

    private fun navigateToOTPFragment() {
        val phoneNumber = viewModel.userInfo.value.phoneNumber
        val userName = viewModel.userInfo.value.fullName
        val user = User(phoneNumber = phoneNumber, userName = userName)
        val action = MBActivationFragmentDirections.actionMBActivationFragmentToVerifyOTPFragment(user)
        findNavController().navigate(action)
    }

    private fun promptInvalidFullName() {
        val prompt =
            VoiceInteractor.Prompt("The name you provided is invalid, please enter a valid name")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptFullName()

            }
        })
    }

    private fun promptInvalidPhoneNumber() {
        val prompt =
            VoiceInteractor.Prompt("The phone number you provided is invalid, please enter a valid phone number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                enterPhoneNumberPrompt()
            }
        })
    }

    private fun promptInvalidEmailAddress() {

        val prompt =
            VoiceInteractor.Prompt("The email address you provided is invalid, please enter a valid email address")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptEmailAddress()

            }
        })
    }

    private fun promptEmailAddress() {
        val prompt = VoiceInteractor.Prompt("What's your email address?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                viewModel.onEvent(
                    MBActivationViewModel.ActivationEvent.UpdateDataState(
                        DataState(
                            isEmailAddress = true
                        )
                    )
                )
                if (!binding.txtInputEtEmailAddress.text.isNullOrBlank()) {
                    binding.txtInputEtEmailAddress.text = null
                }
                binding.txtInputEtEmailAddress.requestFocus()


                lifecycleScope.launch {
                    startListening()
                }

                activity?.stopLocalVoiceInteraction()

            }
        })
    }

    private fun promptFullName() {
        val prompt = VoiceInteractor.Prompt("What's your Full name?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                viewModel.onEvent(
                    MBActivationViewModel.ActivationEvent.UpdateDataState(
                        DataState(
                            isFullName = true
                        )
                    )
                )
                if (!binding.txtInputEtFullName.text.isNullOrBlank()) {
                    binding.txtInputEtFullName.text = null
                }
                binding.txtInputEtFullName.requestFocus()

                lifecycleScope.launch {
                    startListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun welcomePrompt() {
        val prompt = VoiceInteractor.Prompt(
            "Perfect! Let's get started with your mobile banking account activation process."
        )
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                enterPhoneNumberPrompt()
            }
        })
    }

    private fun enterPhoneNumberPrompt() {
        viewModel.onEvent(
            MBActivationViewModel.ActivationEvent.UpdateDataState(
                DataState(
                    isPhoneNumber = true
                )
            )
        )
        val prompt = VoiceInteractor.Prompt("What's your phone Number?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, Bundle()) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtPhoneNumber.text.isNullOrBlank()) {
                    binding.txtInputEtPhoneNumber.text = null
                }
                binding.txtInputEtPhoneNumber.requestFocus()

                lifecycleScope.launch {
                    startListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun initSpeechRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                Log.d(TAG, "onError: SpeechRecognition -> ${error.toString()}")
            }

            override fun onResults(results: Bundle?) {
                if (results != null) {

                    speechRecognizer.stopListening()
                    timer = 0
                    val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val userData = data?.get(0)

                    if (!::dataState.isInitialized)
                        return

                    when {
                        dataState.isPhoneNumber -> {
                            binding.txtInputEtPhoneNumber.setText(userData)
                            binding.txtInputEtPhoneNumber.clearFocus()

                            viewModel.onEvent(
                                MBActivationViewModel.ActivationEvent.SaveData(userData?.trim())
                            )
                        }
                        dataState.isFullName -> {
                            binding.txtInputEtFullName.setText(userData)
                            binding.txtInputEtFullName.clearFocus()

                            viewModel.onEvent(
                                MBActivationViewModel.ActivationEvent.SaveData(userData?.trim())
                            )
                        }
                        dataState.isEmailAddress -> {
                            binding.txtInputEtEmailAddress.setText(userData)
                            binding.txtInputEtEmailAddress.clearFocus()

                            viewModel.onEvent(
                                MBActivationViewModel.ActivationEvent.SaveData(userData?.trim())
                            )

                        }

                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    override fun onPause() {
        super.onPause()
        activity?.stopLocalVoiceInteraction()
    }
}