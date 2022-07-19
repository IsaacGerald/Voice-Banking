package com.example.mywallet.feature_wallet.presentation.auth.login

import android.Manifest
import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.util.RECORD_AUDIO_PERMISSION_CODE
import com.example.mywallet.databinding.FragmentLoginBinding
import com.example.mywallet.core.audio.AudioRecorder
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetAudioFile
import com.example.mywallet.feature_wallet.domain.use_case.GetIsNewUser
import com.example.mywallet.feature_wallet.domain.use_case.GetUsers
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceLogin
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class LoginFragment : Fragment(R.layout.fragment_login), VoiceInteraction {
    private val TAG = LoginFragment::class.java.simpleName
    lateinit var binding: FragmentLoginBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var speechRecognizerIntent: Intent
    private var mediaRecorder: MediaRecorder? = null
    private var recordFile: String? = null
    private var recordedPath: String? = null
    private var isRecording: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    lateinit var voiceState: LoginVoiceState
    lateinit var progressDialog: AlertDialog
    private var isPassport: Boolean = false
    private var timer = 50
    lateinit var recorder: AudioRecorder

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as LoginActivity).dataStore)
    }

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi,
        )
    }

    private val viewModel by viewModels<LoginViewModel> {
        LoginViewModelFactory(
            this,
            GetVoiceLogin(repository),
            GetAudioFile(userPreferences),
            GetUsers(repository),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as LoginActivity).initVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)
        initSpeechRecognizer()


        recorder = AudioRecorder(requireContext())
        binding.btnFaceId.setOnClickListener {
            // navigateToFaceFragment()
        }

        //viewModel.onEvent(LoginViewModel.LoginUiEvent.GetWelcomePrompt)

//        binding.btnStartRecording.setOnClickListener {
//            val audio = viewModel.audio.value
//            recordedPath = audio
//            playAudio()
//        }

        //viewModel.setLoginVoiceState(LoginVoiceState(promptToEnterPasswordOrId = true))
        //viewModel.onEvent(LoginViewModel.LoginUiEvent.AsKForPassportOrId)

        lifecycleScope.launchWhenStarted {
            subscribeUiState()
        }

        //initLoginVoiceState()

    }


    private fun playAudio() {
        showToast("Playing audio....")
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(recordedPath)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return false
        }
    }

    private suspend fun subscribeUiState() {

        viewModel.loginUiState.collectLatest { state ->

            if (state.isLoading) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
            }

            state.loginVoiceState?.let {
                voiceState = state.loginVoiceState
                activity?.startLocalVoiceInteraction(Bundle())
            }

            if (state.navigateToFaceFragment && !state.isLoading) {
                navigateToFaceFragment()
            }

            state.error?.let {
                navigateToFaceFragment()
            }

        }
    }

    private fun navigateToFaceFragment() {
      val actions = LoginFragmentDirections.actionLoginFragment2ToFaceLoginFragment()
        findNavController().navigate(actions)

    }

    private fun showToast(s: String) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()
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


    private suspend fun startListening() {
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
                voiceState.promptSignInOrSignup -> {
                    signInOrSignUp()
                }
                voiceState.promptToEnterPasswordOrId -> {
                    promptEnterIdOrPassport()
                }
                voiceState.enterPasswordOrId -> {
                    enterPassportOrId()
                }
                voiceState.welcomePrompt -> {
                    welcomePrompt()
                }
                voiceState.enterPhoneNumber -> {
                    promptEnterPhoneNumber()
                }
                voiceState.invalidPassportOrId -> {
                    promptEnterInvalidPassportOrId()
                }
                voiceState.invalidPhoneNumber -> {
                    promptEnterInvalidPhoneNumber()
                }
                voiceState.voiceLoginSuccessful -> {
                    promptLoginSuccess()
                }
                voiceState.voiceLoginFailed -> {
                    promptLoginFailed()
                }
            }
        }


    }

    private fun welcomePrompt() {
        val prompt =
            VoiceInteractor.Prompt("Welcome to Eclectics Mobile banking, you must login to continue, would you wish to proceed?")

        val option1 = VoiceInteractor.PickOptionRequest.Option("yes", 1)
        option1.addSynonym("yes proceed")
        option1.addSynonym("yes proceed")
        option1.addSynonym("Proceed")
        option1.addSynonym("proceed")
        option1.addSynonym("continue")
        option1.addSynonym("yes")


        val option2 = VoiceInteractor.PickOptionRequest.Option("no", 1)
        option2.addSynonym("Don't proceed")
        option2.addSynonym("leave")
        option2.addSynonym("No")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    activity.stopLocalVoiceInteraction()
                    when (selections[0].index) {
                        0 -> {
                            lifecycleScope.launch {
                                startRecording()
                            }
                        }
                        1 -> {

                        }
                    }
                    enterPassportOrId()
                }
            }
        })
    }

    private fun promptLoginFailed() {
        val prompt =
            VoiceInteractor.Prompt("Please Login with your face")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                //MoveToFaceLoginFragment
            }
        })
    }

    private fun promptLoginSuccess() {
        val prompt =
            VoiceInteractor.Prompt("Hello, Your login is successFull")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                //MoveToMainActivity
            }
        })
    }

    private fun promptEnterInvalidPhoneNumber() {
        val prompt =
            VoiceInteractor.Prompt("The phone number is invalid, please enter a valid phone number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptEnterPhoneNumber()
            }
        })
    }

    private fun promptEnterInvalidPassportOrId() {
        val prompt: VoiceInteractor.Prompt = if (isPassport) {
            VoiceInteractor.Prompt("The passport is invalid, please enter a valid passport")
        } else {
            VoiceInteractor.Prompt("The ID is invalid, please enter a valid ID")
        }
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                enterPassportOrId()
            }
        })
    }

    private fun promptEnterPhoneNumber() {
        val prompt = VoiceInteractor.Prompt("What is your phone number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
//                if (!binding.txtInputEtPhoneNumber.text.isNullOrBlank()) {
//                    binding.txtInputEtPhoneNumber.text = null
//                }
                //binding.txtInputEtPhoneNumber.requestFocus()
                timer = 50

                lifecycleScope.launch {
                    startRecording()
                    // startListening()
                }
                recorder.startRecording(_internalStorage = true)
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private suspend fun startRecording() {
        recorder.startRecording(_internalStorage = false)

        delay(5000)
        val filePath = recorder.stopRecording()
        showToast("Stopped recording..")
        //val path = LoginFragmentArgs.fromBundle(requireArguments()).enrollmentVoice

        if (filePath != null) {
            //viewModel.validateUserVoice(filePath)
            viewModel.onEvent(LoginViewModel.LoginUiEvent.ValidateUsersVoice(filePath))
        }

        //viewModel.setLoginUIEvent(LoginEvent.ShowProgressBar)
    }

    private fun enterPassportOrId() {
        val prompt: VoiceInteractor.Prompt = if (isPassport) {
            VoiceInteractor.Prompt("What is your passport")
        } else {
            VoiceInteractor.Prompt("What is your Id")
        }
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
//                if (!binding.txtInputEtId.text.isNullOrBlank()) {
//                    binding.txtInputEtId.text = null
//                }
////                timer = 50
//                binding.txtInputEtId.requestFocus()

                lifecycleScope.launch {
                    //startListening()
                    startRecording()
                }
                //recorder.startRecording(_internalStorage = true)
                activity?.stopLocalVoiceInteraction()

            }
        })
    }

    private fun promptEnterIdOrPassport() {
        val prompt = VoiceInteractor.Prompt("Do you want to sign in with your passport or ID?")

        val option1 = VoiceInteractor.PickOptionRequest.Option("Passport", 1)
        option1.addSynonym("Login with my Passport")
        option1.addSynonym("Sign in with my Passport")
        option1.addSynonym("let me sign in with my Passport")
        option1.addSynonym("my passport")
        option1.addSynonym("passport")

        val option2 = VoiceInteractor.PickOptionRequest.Option("ID", 1)
        option2.addSynonym("Login with my ID")
        option2.addSynonym("Sign in with my ID")
        option2.addSynonym("let me sign in with my ID")
        option2.addSynonym("my ID")

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
                            viewModel.setIsPassport(true)
                            isPassport = true
                        }
                        1 -> {
                            viewModel.setIsPassport(false)
                            isPassport = false
                        }
                    }
                    enterPassportOrId()
                }
            }
        })
    }

    private fun signInOrSignUp() {

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
            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                Log.d(TAG, "onError: $error")
            }

            override fun onResults(results: Bundle?) {
                if (results != null) {

                    speechRecognizer.stopListening()
                    timer = 0
                    val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

//                    if (binding.txtInputEtId.hasFocus()) {
//                        // viewModel.saveUser(passportOrId = data!![0])
//                        viewModel.onEvent(LoginViewModel.LoginUiEvent.SaveUser(passportOrId = data!![0]))
//                        binding.txtInputEtId.setText(data[0])
//                        binding.txtInputEtId.clearFocus()
//
//                    } else {
//                        //viewModel.saveUser(phoneNumber = data!![0])
//                        viewModel.onEvent(LoginViewModel.LoginUiEvent.SaveUser(phoneNumber = data!![0]))
//                        binding.txtInputEtId.setText(data[0])
//                        binding.txtInputEtPhoneNumber.clearFocus()
//
//                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit


        })
    }


}