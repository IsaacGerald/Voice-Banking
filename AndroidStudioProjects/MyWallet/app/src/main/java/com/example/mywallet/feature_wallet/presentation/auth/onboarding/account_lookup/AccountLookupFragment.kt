package com.example.mywallet.feature_wallet.presentation.auth.onboarding.account_lookup

import android.app.AlertDialog
import android.app.Dialog
import android.app.VoiceInteractor
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.example.mywallet.databinding.FragmentAccountLookupBinding
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class AccountLookupFragment : Fragment(R.layout.fragment_account_lookup), LoginVoiceInteraction {
    private val TAG = AccountLookupFragment::class.java.simpleName
    lateinit var binding: FragmentAccountLookupBinding
    lateinit var dataState: DataState
    lateinit var viewModel: AccountLookUpViewModel
    lateinit var voiceState: AccountLkUpVoiceState
    lateinit var loadingDialog: LoadingDialog
    lateinit var accountDialog: Dialog
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var timer = 50;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAccountLookupBinding.bind(view)
        viewModel = AccountLookUpViewModel()
        loadingDialog = LoadingDialog(requireActivity())

        viewModel.onEvent(AccountLookUpViewModel.AccountLookUpEvent.GetScreenDescription)

        lifecycleScope.launchWhenStarted {
            initSpeechRecognition()
            subscribeUiState()
        }

//        binding.btnLookup.setOnClickListener {
//           showAccountDialog()
//        }
    }

    private suspend fun subscribeUiState() {
        viewModel.accountLookUpUIState.collect { state ->
            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            state.dataState?.let {
                dataState = it
            }
        }
    }

    private fun showAccountDialog() {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity?.layoutInflater
        builder.setView(inflater?.inflate(R.layout.account_custom_dialog, null))
        builder.setCancelable(true)
        accountDialog = builder.create()
        accountDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

//        val btnCancel = accountDialog.findViewById<Button>(R.id.btn_cancel)
//        val btnConfirm = accountDialog.findViewById<Button>(R.id.btn_confirm)


        accountDialog.show()

    }

    fun dismissAccountDialog() {
        if (::accountDialog.isInitialized) {
            accountDialog.dismiss()
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

    private fun navigateToMobileBankingActivation() {
        val action =
            AccountLookupFragmentDirections.actionAccountLookupFragmentToMBActivationFragment()
        findNavController().navigate(action)
    }

    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.welcomePrompt -> {
                    welcomePrompt()
                }
                voiceState.invalidAccountNumber -> {
                    invalidAccountNumber()
                }
                voiceState.invalidPhoneNumber -> {
                    invalidPhoneNumber()
                }
                voiceState.invalidPassport -> {
                    invalidPassportPrompt()
                }
                voiceState.invalidId -> {
                    invalidIdPrompt()
                }
                voiceState.agreeToTermsAndConditions -> {
                    promptTermsAndConditionAgreement()
                }
                voiceState.activateMobileBanking -> {
                    promptActivateMobileBanking()
                }

            }
        }
    }

    private fun invalidPhoneNumber() {
        val prompt =
            VoiceInteractor.Prompt("The phone number you provided is invalid, please enter a valid phone number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptPhoneNumber()
            }
        })
    }

    private fun promptActivateMobileBanking() {
        val prompt =
            VoiceInteractor.Prompt(
                "We have found an account with the details you provided. " +
                        "Would you like to activate mobile banking for your account?"
            )

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                activity?.stopLocalVoiceInteraction()
                if (confirmed) {
                    dismissAccountDialog()
                    navigateToMobileBankingActivation()
                } else {
                    dismissAccountDialog()
                    //Todo
                }

            }
        })
    }


    private fun promptTermsAndConditionAgreement() {
        val prompt =
            VoiceInteractor.Prompt("Do you agree to the terms and conditions of using this application?")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    activity?.stopLocalVoiceInteraction()
                    lifecycleScope.launch {
                        lookUpAccount()
                    }
                } else {

                }


            }
        })
    }

    private suspend fun lookUpAccount() {
        loadingDialog.startLoadingDialog()

        delay(5000L)
        loadingDialog.dismissDialog()
        viewModel.onEvent(AccountLookUpViewModel.AccountLookUpEvent.ActivateMobileBanking)
        showAccountDialog()

    }

    private fun invalidAccountNumber() {
        val prompt =
            VoiceInteractor.Prompt("The account number is invalid, please enter a valid account number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptAccountNumber()
            }
        })
    }

    private fun invalidPassportPrompt() {
        val prompt =
            VoiceInteractor.Prompt("The passport is invalid, please enter a valid passport number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptPassport()
            }
        })
    }

    private fun invalidIdPrompt() {
        val prompt =
            VoiceInteractor.Prompt("The ID is invalid, please enter a valid ID")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptNationalId()
            }
        })
    }

    private fun welcomePrompt() {
        val prompt =
            VoiceInteractor.Prompt(
                "Welcome to Eclectics Mobile Banking app, " +
                        "To fetch your account, we need to get your details, " +
                        "how would you like to provide your details." +
                        " Using your phone number, national ID, passport or Account number?"
            )

        val option1 = VoiceInteractor.PickOptionRequest.Option("phonenumber", 1);
        option1.addSynonym("Using my phone number")
        option1.addSynonym("Using my phonenumber")
        option1.addSynonym("Phone number")
        option1.addSynonym("phone number")
        option1.addSynonym("Phonenumber")


        val option2 = VoiceInteractor.PickOptionRequest.Option("national id", 1);
        option2.addSynonym("ID")
        option2.addSynonym("I D")
        option2.addSynonym("National ID")
        option2.addSynonym("My national ID")


        val option3 = VoiceInteractor.PickOptionRequest.Option("Account number", 1);
        option3.addSynonym("My account number")
        option3.addSynonym("account number")
        option3.addSynonym("with my account number")
        option3.addSynonym("using my account number")

        val option4 = VoiceInteractor.PickOptionRequest.Option("passport", 1);
        option4.addSynonym("Passport")
        option4.addSynonym("my passport")
        option4.addSynonym("using my passport")
        option4.addSynonym("with my passport")


        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(
                prompt,
                arrayOf(option1, option2),
                null
            ) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    when (selections[0].index) {
                        0 -> {
                            //phoneNumber
                            promptPhoneNumber()
                        }
                        1 -> {
                            //nationalID
                            promptNationalId()
                        }
                        2 -> {
                            //account Number
                            promptAccountNumber()
                        }
                        3 -> {
                            //passport
                            promptPassport()
                        }
                    }
                }
            }
        })
    }

    private fun promptPassport() {
        viewModel.onEvent(
            AccountLookUpViewModel.AccountLookUpEvent.GetDataState(
                DataState(
                    isPassport = true
                )
            )
        )
        val prompt =
            VoiceInteractor.Prompt("Whats your passport?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtIdPassport.text.isNullOrBlank()){
                    binding.txtInputEtIdPassport.text = null
                }
                binding.txtInputEtIdPassport.requestFocus()

                lifecycleScope.launch {
                    startListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun promptAccountNumber() {
        viewModel.onEvent(
            AccountLookUpViewModel.AccountLookUpEvent.GetDataState(
                DataState(
                    isAccount = true
                )
            )
        )
        val prompt =
            VoiceInteractor.Prompt("Whats your account number?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtAccountNumber.text.isNullOrBlank()){
                    binding.txtInputEtAccountNumber.text = null
                }
                binding.txtInputEtAccountNumber.requestFocus()
                lifecycleScope.launch {
                    startListening()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun promptNationalId() {
        viewModel.onEvent(
            AccountLookUpViewModel.AccountLookUpEvent.GetDataState(
                DataState(
                    isNationalId = true
                )
            )
        )
        val prompt =
            VoiceInteractor.Prompt("Whats your national ID?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {


            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)

                    if (!binding.txtInputEtIdPassport.text.isNullOrBlank()) {
                        binding.txtInputEtIdPassport.text = null
                    }
                    binding.txtInputEtIdPassport.requestFocus()

                    lifecycleScope.launch {
                        startListening()
                    }
                    activity?.stopLocalVoiceInteraction()

            }
        })
    }

    private fun promptPhoneNumber() {
        viewModel.onEvent(
            AccountLookUpViewModel.AccountLookUpEvent.GetDataState(
                DataState(
                    isPhoneNumber = true
                )
            )
        )
        val prompt =
            VoiceInteractor.Prompt("Whats your phone number?")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                if (!binding.txtInputEtPhoneNumber.text.isNullOrBlank()){
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
                                AccountLookUpViewModel.AccountLookUpEvent.SaveData(
                                    userData
                                )
                            )
                        }
                        dataState.isNationalId -> {
                            binding.txtInputEtIdPassport.setText(userData)
                            binding.txtInputEtIdPassport.clearFocus()

                            viewModel.onEvent(
                                AccountLookUpViewModel.AccountLookUpEvent.SaveData(
                                    userData
                                )
                            )
                        }
                        dataState.isPassport -> {
                            binding.txtInputEtIdPassport.setText(userData)
                            binding.txtInputEtIdPassport.clearFocus()

                            viewModel.onEvent(
                                AccountLookUpViewModel.AccountLookUpEvent.SaveData(
                                    userData
                                )
                            )

                        }
                        dataState.isAccount -> {
                            binding.txtInputEtAccountNumber.setText(userData)
                            binding.txtInputEtAccountNumber.clearFocus()

                            viewModel.onEvent(
                                AccountLookUpViewModel.AccountLookUpEvent.SaveData(
                                    userData
                                )
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