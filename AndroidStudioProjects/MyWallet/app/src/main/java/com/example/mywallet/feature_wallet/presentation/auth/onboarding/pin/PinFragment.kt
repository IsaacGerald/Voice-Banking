package com.example.mywallet.feature_wallet.presentation.auth.onboarding.pin

import android.app.VoiceInteractor
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.databinding.FragmentPinBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserPin
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import com.example.mywallet.feature_wallet.presentation.auth.login.pin.VoicePinState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class PinFragment : Fragment(R.layout.fragment_pin), LoginVoiceInteraction {
    lateinit var binding: FragmentPinBinding
    private var enterPin: Boolean = false
    private var confirmPin: Boolean = false
    private var completeValidation: Boolean = false
    private var isIncorrectPin: Boolean = false
    private var isInvalidPin: Boolean = false
    private var isConfirmPin:  Boolean = false
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var speechRecognizerIntent: Intent
    private var timer = 50;

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as OnBoardingActivity).dataStore)
    }

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi,
        )
    }

    private val viewModel by viewModels<PinViewModel> {
        PinViewModelFactory(
            this,
            SaveUserPin(userPreferences),
             SaveUser(repository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPinBinding.bind(view)

        val user = PinFragmentArgs.fromBundle(requireArguments()).user
        viewModel.setUser(user)

        lifecycleScope.launchWhenStarted {
            subscribePinUiEvent()
        }
        initSpeechRecognizer()
        viewModel.setVoiceState(VoicePinState(enterPin = true))

        initVoiceState()

    }


    private fun subscribePinUiEvent() {
        viewModel.pinUiEvent.onEach { event ->
            when (event) {
                is PinViewModel.UIEvent.PinDoesNotMatch -> {
                    viewModel.setVoiceState(VoicePinState(isIncorrectPin = true))
                }
                is PinViewModel.UIEvent.CompleteVerification -> {
                    viewModel.setVoiceState(VoicePinState(completeValidation = true))
                }
                is PinViewModel.UIEvent.ConfirmPin -> {
                    viewModel.setVoiceState(VoicePinState(confirmPin = true))
                }
                is PinViewModel.UIEvent.PinIsInvalid -> {
                    isConfirmPin = event.isConfirmPin
                    viewModel.setVoiceState(VoicePinState(isInvalidPin = true))

                }
            }

        }.launchIn(lifecycleScope)
    }

    private fun initVoiceState() {
        viewModel.voiceState.observe(viewLifecycleOwner) { state ->
            enterPin = state.enterPin
            confirmPin = state.confirmPin
            completeValidation = state.completeValidation
            isIncorrectPin = state.isIncorrectPin
            isInvalidPin = state.isInvalidPin

            activity?.startLocalVoiceInteraction(Bundle())
        }

    }

    override fun isVoiceInteractionStarted() {

        if (enterPin) {
            promptEnterPin()
        } else if (confirmPin) {
            promptConfirmPin()
        } else if (completeValidation) {
            promptCompleteValidation()
        } else if (isIncorrectPin) {
            promptPinIsIncorrect()
        } else if (isInvalidPin) {
            promptPinIsInvalid()
        }

    }

    private fun promptPinIsIncorrect() {
        val prompt = VoiceInteractor.Prompt("The pin does not match, enter a valid pin")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptEnterPin()
            }
        })
    }

    private fun promptCompleteValidation() {
        val prompt =
            VoiceInteractor.Prompt("Your registration is completed successfully, please login to continue")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                navigateToLoginActivity()
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun navigateToLoginActivity() {
//        val action = PinFragmentDirections.actionPinFragmentToLoginFragment(null)
//        findNavController().navigate(action)
    }

    private fun promptConfirmPin() {
        val prompt = VoiceInteractor.Prompt("Please confirm your pin")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtConfirmPin.text.isNullOrBlank()){
                    binding.txtInputEtConfirmPin.text = null
                }
                binding.txtInputEtConfirmPin.requestFocus()
                timer = 50
                lifecycleScope.launch {
                    onStartListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun promptPinIsInvalid() {
        val prompt = VoiceInteractor.Prompt("The pin is Invalid, please enter a valid pin")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (isConfirmPin) {
                    promptConfirmPin()
                } else {
                    promptEnterPin()
                }

            }
        })
    }

    private fun promptEnterPin() {
        val prompt = VoiceInteractor.Prompt("Please enter your pin")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtPin.text.isNullOrBlank()){
                    binding.txtInputEtPin.text = null
                }
                binding.txtInputEtPin.requestFocus()
                timer = 50
                lifecycleScope.launch {
                    onStartListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private suspend fun onStartListening() {
        withContext(Dispatchers.Main) {
            while (timer > 1) {
                timer--
                speechRecognizer.startListening(speechRecognizerIntent)
                delay(1000)
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {

            }

            override fun onBeginningOfSpeech() {

            }

            override fun onRmsChanged(rmsdB: Float) {

            }

            override fun onBufferReceived(buffer: ByteArray?) {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(error: Int) {

            }

            override fun onResults(results: Bundle?) {
                if (results == null) {
                    return
                }
                speechRecognizer.stopListening()
                timer = 0
                val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (binding.txtInputEtPin.hasFocus()) {
                    viewModel.savePin(data!![0], false)
                    binding.txtInputEtPin.setText(data[0].toString())
                    binding.txtInputEtPin.clearFocus()


                } else {
                    viewModel.savePin(data!![0], true)
                    binding.txtInputEtConfirmPin.setText(data[0].toString())
                    binding.txtInputEtConfirmPin.clearFocus()

                }

            }

            override fun onPartialResults(partialResults: Bundle?) {

            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }
        })
    }


}