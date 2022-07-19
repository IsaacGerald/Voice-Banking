package com.example.mywallet.feature_wallet.presentation.auth.onboarding.signup

import android.app.AlertDialog
import android.app.Dialog
import android.app.VoiceInteractor
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.util.Constants
import com.example.mywallet.databinding.FragmentSignupBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserCredentials
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.signup.SignUpViewModel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class SignupFragment : Fragment(R.layout.fragment_signup), LoginVoiceInteraction {
    private val TAG = SignupFragment::class.java.simpleName
    lateinit var binding: FragmentSignupBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isUsername = false
    private var isPhoneNumber = false
    private var isPassportId = false
    lateinit var credentialState: CredentialState
    private var timer = 50
    lateinit var voiceState: SignupVoiceState
    lateinit var alertDialog: AlertDialog.Builder
    lateinit var dialog: Dialog
    private val accounts = arrayOf("Personal account", "Savings account", "Fixed deposit account")

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

    private val viewModel by viewModels<SignUpViewModel> {
        SignUpViewModelFactory(
            this, SaveUserCredentials(userPreferences), SaveUser(repository)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignupBinding.bind(view)

        (requireActivity() as OnBoardingActivity).initLocalVoiceInteraction(this)
        initSpeechRecognition()

        viewModel.onEvent(SignupUiEvent.InitVoicePrompt)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            Log.d(TAG, "onViewCreated: User -> $user.")
        }

        lifecycleScope.launchWhenStarted {
            subscribeSignUpUiState()
        }

    }


    private suspend fun subscribeSignUpUiState() {
        viewModel.signUpUiState.collectLatest { state ->
            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            state.credentialState?.let {
                credentialState = it
            }
        }


    }


    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.isPassportOrId -> {
                    promptForIdOrPassport()
                }
                voiceState.isUsername -> {
                    promptForUsername()
                }
                voiceState.isPhoneNumber -> {
                    promptForPhoneNumber()
                }
                voiceState.enterPassportOrId -> {
                    promptEnterIdOrPassport()
                }
                voiceState.confirmAccount -> {
                    promptConfirmCreateAccount()
                }
                voiceState.enableBiometrics -> {
                    promptToEnableBiometrics()
                }
                voiceState.createAccount -> {
                    promptCreateAccountType()
                }
                voiceState.createAnotherAccount -> {
                    promptOpenAnotherAccount()
                }
                voiceState.passportIsInvalid -> {
                    promptPassportIsInvalid()
                }
                voiceState.idIsInvalid -> {
                    promptIdIsInvalid()
                }
                voiceState.phoneNumberIsInvalid -> {
                    promptForPhoneNumberIsInvalid()
                }
            }
        }

    }

    private fun promptForPhoneNumberIsInvalid() {
        val prompt =
            VoiceInteractor.Prompt("The phoneNumber is invalid, please enter a valid phone number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptForPhoneNumber()

            }
        })
    }

    private fun promptIdIsInvalid() {
        val prompt = VoiceInteractor.Prompt("The ID is invalid, please enter a valid ID")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptEnterIdOrPassport()

            }
        })
    }

    private fun promptPassportIsInvalid() {
        val prompt =
            VoiceInteractor.Prompt("The passport is invalid, please enter a valid passport")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptEnterIdOrPassport()

            }
        })
    }


    private fun promptForUsername() {
        val prompt = VoiceInteractor.Prompt("What is your username")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                stopVoiceInteraction()
                if (!binding.txtInputEtUsername.text.isNullOrBlank()){
                    binding.txtInputEtUsername.text = null
                }
                binding.txtInputEtUsername.requestFocus()
                lifecycleScope.launch {
                    timer = 50
                    startListening()
                }
                stopVoiceInteraction()
            }
        })
    }

    private fun promptForIdOrPassport() {

        val prompt =
            VoiceInteractor.Prompt("Welcome to my wallet app, do you wish to sign up with your Id or Passport")
        val option1 = VoiceInteractor.PickOptionRequest.Option("Passport", 1)
        option1.addSynonym("Register with my passport")
        option1.addSynonym("Sign up with my passport")
        option1.addSynonym("My passport")
        option1.addSynonym("I want to sign up with my passport")
        option1.addSynonym("I want to Register with my passport")

        val option2 = VoiceInteractor.PickOptionRequest.Option("I-D", 1)
        option2.addSynonym("Register with my I-D")
        option2.addSynonym("Sign up with my I-D")
        option2.addSynonym("My I D")
        option2.addSynonym("I want to sign up with my I-D")
        option2.addSynonym("I want to Register with my i D")
        option2.addSynonym("I D")


        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    when (selections[0].index) {
                        0 -> {

                            stopVoiceInteraction()
                            viewModel.onEvent(SignupUiEvent.GetPassportOrId(passportOrId = Constants.PASSPORT))

                        }
                        1 -> {

                            stopVoiceInteraction()
                            viewModel.onEvent(SignupUiEvent.GetPassportOrId(passportOrId = Constants.ID))

                        }

                    }

                }
            }
        })
    }


    private fun stopVoiceInteraction() {
        activity?.stopLocalVoiceInteraction()
    }

    private fun promptConfirmCreateAccount() {
        val prompt =
            VoiceInteractor.Prompt("Your credentials are saved, do you wish to create an account?")
        val option1 = VoiceInteractor.PickOptionRequest.Option("Yes", 1)
        option1.addSynonym("proceed")
        option1.addSynonym("Create one")
        option1.addSynonym("Make it so")
        option1.addSynonym("Proceed to create")
        option1.addSynonym("I don't have an account")
        option1.addSynonym("create an account")
        option1.addSynonym("create one")
        option1.addSynonym("yes")

        val option2 = VoiceInteractor.PickOptionRequest.Option("no", 1)
        option2.addSynonym("I already have an account")
        option2.addSynonym("Skip")
        option2.addSynonym("I have an account")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    when (selections[0].index) {
                        0 -> {
                            promptCreateAccountType()
                        }
                        1 -> {
                            promptToEnableBiometrics()
                        }
                    }
                }
            }
        })
    }


    private fun promptCreateAccountType() {
        showAccountDialog()
        val prompt =
            VoiceInteractor.Prompt("Which account do want to create, personal account, savings account or fixed deposit account")

        val option1 = VoiceInteractor.PickOptionRequest.Option("personal account", 1)
        option1.addSynonym("personal")

        val option2 = VoiceInteractor.PickOptionRequest.Option("Savings account", 1)
        option2.addSynonym("Savings")

        val option3 = VoiceInteractor.PickOptionRequest.Option("Fixed deposit account", 1)
        option2.addSynonym("fixed account")
        option2.addSynonym("fixed deposit account")


        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2, option3), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    when (selections[0].index) {
                        0 -> {

                            viewModel.onEvent(
                                SignupUiEvent.SaveUserCredentials(
                                    account = listOf(
                                        Constants.PERSONAL_ACCOUNT
                                    )
                                )
                            )
                        }
                        1 -> {

                            viewModel.onEvent(
                                SignupUiEvent.SaveUserCredentials(
                                    account = listOf(
                                        Constants.SAVINGS_ACCOUNT
                                    )
                                )
                            )
                        }
                        2 -> {

                            viewModel.onEvent(
                                SignupUiEvent.SaveUserCredentials(
                                    account = listOf(
                                        Constants.FIXED_DEPOSIT_ACCOUNT
                                    )
                                )
                            )
                        }
                    }
                    closeAccountDialog()
                    promptOpenAnotherAccount()
                }
            }
        })

    }

    private fun showAccountDialog() {

        alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Accounts")
        alertDialog.setItems(accounts) { dialog, which ->
            Toast.makeText(context, "${accounts[which]} is clicked", Toast.LENGTH_SHORT).show()
        }
        dialog = alertDialog.create()
        dialog.show()

    }

    private fun closeAccountDialog() {
        if (::dialog.isInitialized) {
            dialog.dismiss()
        }
    }

    private fun promptOpenAnotherAccount() {
        val prompt =
            VoiceInteractor.Prompt("we are creating your new account, do wish to open another account?")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    promptCreateAccountType()
                } else {
                    promptToEnableBiometrics()
                }
            }
        })
    }


    private fun promptToEnableBiometrics() {
        val prompt =
            VoiceInteractor.Prompt(
                "To make sure your banking is secure, " +
                        "you must enable one of the sign up biometrics, which one do you wish to enable Facial or Voice Recognition"
            )

        val option1 = VoiceInteractor.PickOptionRequest.Option("Facial", 1)
        option1.addSynonym("Facial recognition")
        option1.addSynonym("enable facial recognition")
        option1.addSynonym("let me enable  facial recognition")
        option1.addSynonym("enable facial recognition")
        option1.addSynonym("enable facial")
        option1.addSynonym("enable face")
        option1.addSynonym("facial")
        option1.addSynonym("face")


        val option2 = VoiceInteractor.PickOptionRequest.Option("voice", 1)
        option2.addSynonym("Voice recognition")
        option2.addSynonym("Enable voice recognition")
        option2.addSynonym("let me enable voice recognition")
        option2.addSynonym("Enable voice recognition")
        option2.addSynonym("Enable voice")

        val option3 = VoiceInteractor.PickOptionRequest.Option("any", 1)
        option2.addSynonym("any")
        option2.addSynonym("enable any")


        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2, option3), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    when (selections[0].index) {
                        0 -> {
                            //Enable voice recognition
                            activity?.stopLocalVoiceInteraction()
                            navigateToFaceFragment()

                        }
                        1 -> {
                            //Enable face recognition
                            activity?.stopLocalVoiceInteraction()
                            navigateToAudioFragment()
                        }
                        2 -> {
                            //enable voice recognition

                        }
                    }
                }
            }
        })

    }

    private fun navigateToFaceFragment() {
//        val action = SignupFragmentDirections.actionSignupFragmentToFaceFragment(
//            Constants.AUDIO_NOT_REGISTERED,
//            viewModel.user.value
//        )
//        findNavController().navigate(action)


    }

    private fun navigateToAudioFragment() {
//        val action = SignupFragmentDirections.actionSignupFragmentToRecordAudioFragment(
//            Constants.FACE_NOT_REGISTERED,
//            viewModel.user.value
//        )
//        findNavController().navigate(action)
    }


    private fun promptEnterIdOrPassport() {
        Toast.makeText(context, "What is your ${viewModel.passportId.value}", Toast.LENGTH_SHORT)
            .show()
        val prompt = VoiceInteractor.Prompt("What is your ${viewModel.passportId.value}")

        (requireActivity() as OnBoardingActivity).voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtId.text.isNullOrBlank()){
                    binding.txtInputEtId.text = null
                }
                binding.txtInputEtId.requestFocus()

                timer = 50
                lifecycleScope.launch {
                    startListening()
                }
                stopVoiceInteraction()
            }
        })


    }


    private fun promptForPhoneNumber() {
        val prompt = VoiceInteractor.Prompt("What is your Phone Number")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtPhoneNumber.text.isNullOrBlank()){
                    binding.txtInputEtPhoneNumber.text = null
                }
                binding.txtInputEtPhoneNumber.requestFocus()

                timer = 50
                lifecycleScope.launch {
                    startListening()
                }
                stopVoiceInteraction()
            }
        })
    }


    private suspend fun startListening() {
        withContext(Dispatchers.Main) {
            while (timer > 1) {
                timer--
                speechRecognizer.startListening(speechRecognizerIntent)
                delay(1000)
            }
        }
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

                    if (!::credentialState.isInitialized)
                        return

                    when {
                        credentialState.isPassportOrId -> {
                            //bind data to view
                            Log.d(TAG, "onResults: passportOrId -> $userData")
                            binding.txtInputEtId.clearFocus()
                            isPassportId = false
                            when (viewModel.passportId.value) {
                                Constants.PASSPORT -> {
                                    Log.d(TAG, "onResults: Password -> IsPassport")
                                    viewModel.onEvent(SignupUiEvent.SaveUserCredentials(passport = userData))
                                    binding.txtInputEtId.setText(userData)
                                }
                                Constants.ID -> {
                                    Log.d(TAG, "onResults: Id -> IsId")
                                    viewModel.onEvent(SignupUiEvent.SaveUserCredentials(id = userData))
                                    binding.txtInputEtId.setText(userData)
                                }
                            }

                        }
                        credentialState.isPhoneNumber -> {
                            //bind data to view
                            Log.d(TAG, "onResults: phoneNumber -> $userData")
                            binding.txtInputEtPhoneNumber.clearFocus()
                            isPhoneNumber = false
                            binding.txtInputEtPhoneNumber.setText(userData)
                            viewModel.onEvent(SignupUiEvent.SaveUserCredentials(phoneNumber = userData))

                        }
                        credentialState.isUserName -> {
                            //bind data to view
                            binding.txtInputEtUsername.clearFocus()
                            isUsername = false
                            Log.d(TAG, "onResults: DOB -> $userData")
                            viewModel.onEvent(SignupUiEvent.SaveUserCredentials(userName = userData))
                            binding.txtInputEtUsername.setText(userData)

                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }


}