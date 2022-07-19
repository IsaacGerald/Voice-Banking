package com.example.mywallet.feature_wallet.presentation.auth.login.pin

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mywallet.R
import com.example.mywallet.databinding.FragmentPinLoginBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetUserPin
import com.example.mywallet.feature_wallet.domain.use_case.GetUsers
import com.example.mywallet.feature_wallet.presentation.wallet.MainActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class LoginPinFragment : Fragment(R.layout.fragment_pin_login), LoginVoiceInteraction {
    lateinit var binding: FragmentPinLoginBinding
    private var enterPin: Boolean = false
    private var confirmPin: Boolean = false
    private var completeValidation: Boolean = false
    private var isIncorrectPin: Boolean = false
    private var isInvalidPin: Boolean = false
    private var isConfirmPin: Boolean = false
    lateinit var progressDialog: AlertDialog
    private var isIdOrPin: String? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var speechRecognizerIntent: Intent
    private var timer = 50;

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi,
        )
    }

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as MainActivity).dataStore)
    }

    private val viewModel by viewModels<LoginPinViewModel> {
        LoginPinViewModelFactory(
            this,
            GetUserPin(userPreferences), GetUsers(repository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPinLoginBinding.bind(view)



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
                is LoginPinViewModel.UIEvent.PinDoesNotMatch -> {
                    dismissProgressDialog()
                    viewModel.setVoiceState(VoicePinState(isIncorrectPin = true))
                }
                is LoginPinViewModel.UIEvent.CompleteVerification -> {
                    dismissProgressDialog()
                    viewModel.setVoiceState(VoicePinState(completeValidation = true))
                }
                is LoginPinViewModel.UIEvent.PinIsInvalid -> {
                    viewModel.setVoiceState(VoicePinState(isInvalidPin = true))

                }
                LoginPinViewModel.UIEvent.ShowProgressBar -> {
                    showProgressDialog()
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

    private fun showProgressDialog() {
        val dialogLayout =
            LayoutInflater.from(requireContext()).inflate(R.layout.progess_layout, null)
        progressDialog = AlertDialog
            .Builder(requireContext())
            .setView(dialogLayout)
            .create()

        progressDialog.show()

    }

    private fun dismissProgressDialog() {
        if (::progressDialog.isInitialized) {
            progressDialog.dismiss()
        }
    }

    override fun isVoiceInteractionStarted() {

        if (enterPin) {
            promptEnterPin()
        } else if (completeValidation) {
            promptCompleteValidation()
        } else if (isIncorrectPin) {
            promptPinIsIncorrect()
        } else if (isInvalidPin) {
            promptPinIsInvalid()
        }

    }

    private fun promptPinIsIncorrect() {
        val prompt = VoiceInteractor.Prompt("The pin is Incorrect, please enter a valid pin")

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
            VoiceInteractor.Prompt("Your login is successful")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                showToast("Login is successful")
                navigateMainActivity()
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun navigateMainActivity() {
        showToast("Navigate to main activity")
    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }


    private fun promptPinIsInvalid() {
        val prompt = VoiceInteractor.Prompt("The pin is Invalid, please enter a valid pin")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)

                promptEnterPin()


            }
        })
    }

    private fun promptEnterPin() {
        val prompt = VoiceInteractor.Prompt("What is your pi?n")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtPin.text.isNullOrBlank()) {
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

                viewModel.savePin(data!![0])
                binding.txtInputEtPin.setText(data[0].toString())
                binding.txtInputEtPin.clearFocus()


            }

            override fun onPartialResults(partialResults: Bundle?) {

            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }
        })
    }


}