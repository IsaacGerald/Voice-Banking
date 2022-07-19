package com.example.mywallet.feature_wallet.presentation.auth.onboarding.voice

import android.Manifest
import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.audio.AudioRecorder
import com.example.mywallet.core.recognition.RecognitionProgressView
import com.example.mywallet.core.recognition.adapters.RecognitionListenerAdapter
import com.example.mywallet.core.util.Constants
import com.example.mywallet.databinding.FragmentRecordAudioBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetVoiceRegistration
import com.example.mywallet.feature_wallet.domain.use_case.SaveAudioFile
import com.example.mywallet.feature_wallet.domain.use_case.SaveUser
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class RecordAudioFragment : Fragment(R.layout.fragment_record_audio), LoginVoiceInteraction {
    private val TAG = RecordAudioFragment::class.java.simpleName
    lateinit var binding: FragmentRecordAudioBinding
    private var speechRecognizer: SpeechRecognizer? = null
    lateinit var speechRecognizerIntent: Intent
    lateinit var recorder: AudioRecorder
    private var timer = 50
    private var recorderFilePath: String? = null
    private var statementIterator: Int = 0
    private var _promptRepeatWords = false
    private var _repeatTextWords = false
    private var outPutDir: File? = null
    private var _endVoiceAuthentication = false
    private var _voiceRegistrationFailed = false
    private var speechResults: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var filePath: String? = null
    lateinit var progressDialog: AlertDialog
    private lateinit var recognitionProgressView: RecognitionProgressView
    private lateinit var colors: IntArray
    private val heights = intArrayOf(25, 29, 23, 28, 21)

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

    private val viewModel by viewModels<AudioViewModel> {
        RecordAudioViewModelFactory(
            this, GetVoiceRegistration(repository), SaveAudioFile(userPreferences), SaveUser(repository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecordAudioBinding.bind(view)
        recorder = AudioRecorder(requireContext())

        outPutDir = getOutputDirectory()

        val user = RecordAudioFragmentArgs.fromBundle(requireArguments()).user
        if (user != null) {
            viewModel.saveUser(user)
        }

        initColors()
        initVoiceState()
        initAudioPath()

        lifecycleScope.launch {
            subscribeAudioUiEvent()
        }

        recognitionProgressView = binding.recognitionView
        initializeSpeechRecognizer()

        viewModel.statement.observe(viewLifecycleOwner) { statement ->
            binding.txtRecord.text = statement
        }

        binding.btnStartListening.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission()
            } else {
                binding.btnStartListening.text = "Started listening..."
                startRecognition()
                // recorder.startRecording(_internalStorage = true)
                recognitionProgressView.postDelayed({ startRecognition() }, 50)
            }
        }

        binding.reset.setOnClickListener {
            //resetRecognition()
            playAudio()

        }

        viewModel.setVoiceState(VoiceAudioState(promptRepeatWords = true))


    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs!!.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity?.filesDir!!
    }

    private fun subscribeAudioUiEvent() {
        viewModel.audioUiEvent.onEach { event ->
            when (event) {
                is AudioViewModel.AudioUiEvent.ShowProgressBar -> {
                    showProgressDialog()
                    delay(4000)
                    dismissProgressDialog()
                    viewModel.setVoiceState(VoiceAudioState(endVoiceAuthentication = true))
                }
                is AudioViewModel.AudioUiEvent.VoiceRegistrationSuccessFull -> {
                    dismissProgressDialog()
                    //viewModel.setVoiceState(VoiceAudioState(endVoiceAuthentication = true))
                }
                is AudioViewModel.AudioUiEvent.VoiceRegistrationFailed -> {
                    dismissProgressDialog()
                    //viewModel.setVoiceState(VoiceAudioState(endVoiceAuthentication = true))
                    //viewModel.setVoiceState(VoiceAudioState(voiceRegistrationFailed = true))
                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun initAudioPath() {
        viewModel.audioPath.observe(viewLifecycleOwner) { audio ->
            audio.forEach {
                Log.d(TAG, "initAudioPath: Audio -> $it ")
            }
        }
    }

    private fun startListening() {
        binding.btnStartListening.text = "Started listening..."
        startRecognition()
        recognitionProgressView.postDelayed({ startRecognition() }, 50)
    }

    private fun initVoiceState() {
        viewModel.voiceState.observe(viewLifecycleOwner) { state ->
            _promptRepeatWords = state.promptRepeatWords
            _repeatTextWords = state.repeatTextWords
            _endVoiceAuthentication = state.endVoiceAuthentication
            _voiceRegistrationFailed = state.voiceRegistrationFailed

            activity?.startLocalVoiceInteraction(Bundle())
        }
    }

    private fun playAudio() {
        showToast("Playing audio....")
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(recorderFilePath)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
    }

    private fun resetRecognition() {
        binding.btnStartListening.text = "Start listening"
        recognitionProgressView.stop()
        recognitionProgressView.play()
    }

    private fun startRecognition() {
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            activity?.packageName
        );

        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer?.startListening(speechRecognizerIntent)


    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognitionProgressView.setSpeechRecognizer(speechRecognizer)
        recognitionProgressView.apply {
            setColors(colors)
            setBarMaxHeightsInDp(heights)
            setCircleRadiusInDp(2)
            setSpacingInDp(8)
            setIdleStateAmplitudeInDp(3)
            setRotationRadiusInDp(10)
            play()
        }

        recognitionProgressView.setRecognitionListener(object : RecognitionListenerAdapter() {
            override fun onResults(results: Bundle?) {
                super.onResults(results)
                Log.d(TAG, "onResults: isCalled..")
                binding.btnStartListening.text = "Start Listening"
                showResults(results)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                super.onPartialResults(partialResults)
                Log.d(TAG, "onPartialResults: isCalled..")

            }


            override fun onError(error: Int) {
                super.onError(error)
                showToast("Error -> ${error.toString()}")
                Log.d(TAG, "onError: Is called..")
            }

        })

    }

    private suspend fun getStatement() {
        for (i in 0..2) {
            if (viewModel.statementIndex.value!! < 2) {
                viewModel.nextStatement()
                viewModel.getStatement()
                delay(3000)
            } else {
                viewModel.resetIndex()
                val audioPath = recorder.stopRecording()
                val file = filePath?.let { File(it) }
                //val file = recorder.getFileName()?.let { File(outPutDir, it) }
                if (audioPath != null) {
                    filePath = audioPath
                    viewModel.getVoiceValidation(audioPath)
                }
//                file?.let {
//                    viewModel.getVoiceValidation(it)
//                    Log.d(TAG, "getStatement: File -> $it")
//                }

                recorderFilePath = filePath
                resetRecognition()
            }
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            Toast.makeText(context, "Requires RECORD_AUDIO permission", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO),
                111
            )
        }
    }

    private fun showResults(results: Bundle?) {
        if (results != null) {

//                speechRecognizer?.stopListening()
//                recognitionProgressView.stop()
            recorder.startRecording(_internalStorage = true)
            lifecycleScope.launch {
                getStatement()
            }


//            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//            speechResults = matches?.get(0)?.toString()
//            resetRecognition()
//            Toast.makeText(context, matches!![0], Toast.LENGTH_LONG).show()
//            if (viewModel.statementIndex.value!! <= 2){
//                viewModel.setVoiceState(VoiceAudioState(repeatTextWords = true))
//                viewModel.nextStatement()
//            }else{
//                viewModel.resetIndex()
//                viewModel.setVoiceState(VoiceAudioState(endVoiceAuthentication = true))
//            }
//
//            val audioPath: String? = recorder.stopRecording()
//            audioPath?.let {
//                viewModel.saveAudio(it)
//            }

        } else {
            showToast(" Result is null")
        }

    }

    private fun initColors() {
        colors = intArrayOf(
            ContextCompat.getColor(requireContext(), R.color.color1),
            ContextCompat.getColor(requireContext(), R.color.color2),
            ContextCompat.getColor(requireContext(), R.color.color3),
            ContextCompat.getColor(requireContext(), R.color.color4),
            ContextCompat.getColor(requireContext(), R.color.color5)
        )
    }

    override fun onDestroyView() {
        if (speechRecognizer != null) {
            speechRecognizer!!.destroy();
        }
        super.onDestroyView()
    }


    private fun showToast(toastMsg: String) {
        Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()

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


    private fun promptToRepeatTextWords() {
        val prompt =
            VoiceInteractor.Prompt("Please Repeat the words in text to register your voice")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.stopLocalVoiceInteraction()
                showToast("Started..")
                startListening()

            }
        })

    }

    private fun repeatTextWords() {
        val prompt = VoiceInteractor.Prompt("${viewModel.statement.value}")
        activity?.voiceInteractor?.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                startListening()
                activity?.stopLocalVoiceInteraction()
            }
        })
    }


    private fun completeVoiceRegistration() {
        val args = RecordAudioFragmentArgs.fromBundle(requireArguments()).isFaceOrAudiRegistered
        if (args == Constants.FACE_NOT_REGISTERED) {
            promptFaceRegistration()
        } else {
            promptCompleteRegistration()
        }


    }

    private fun promptFaceRegistration() {
        val prompt =
            VoiceInteractor.Prompt("Your voice is registered successfully, would you wish to register  your face?")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    navigateToFaceFragment()
                } else {
                    navigateToPinFragment()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })
    }

    private fun navigateToPinFragment() {
        val user = RecordAudioFragmentArgs.fromBundle(requireArguments()).user
       // val newUser = viewModel.user.value
        val actions = RecordAudioFragmentDirections.actionRecordAudioFragmentToPinFragment(user!!)
        findNavController().navigate(actions)

    }

    private fun navigateToFaceFragment() {
       // val args = RecordAudioFragmentArgs.fromBundle(requireArguments()).user
        val newUser = viewModel.user.value
        Log.d(TAG, "navigateToFaceFragment: $newUser")
        val action = RecordAudioFragmentDirections.actionRecordAudioFragmentToFaceFragment(
            Constants.AUDIO_REGISTERED,
            newUser
        )
        findNavController().navigate(action)
    }

    private fun promptCompleteRegistration() {
        val prompt = VoiceInteractor.Prompt("Your voice registration is completed successfully")
        activity?.voiceInteractor?.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.stopLocalVoiceInteraction()
                navigateToPinFragment()

            }
        })
    }

    override fun isVoiceInteractionStarted() {
        when {
            _promptRepeatWords -> {
                promptToRepeatTextWords()
            }
            _repeatTextWords -> {
                repeatTextWords()
            }
            _endVoiceAuthentication -> {
                completeVoiceRegistration()
            }
            _voiceRegistrationFailed -> {
                promptVoiceValidationFailed()
            }
        }

    }

    private fun promptVoiceValidationFailed() {
        val prompt = VoiceInteractor.Prompt("Voice validation failed, would you want to try again")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    promptToRepeatTextWords()
                } else {
                    moveToFaceOrPinFragment()
                }
            }
        })
    }

    private fun moveToFaceOrPinFragment() {
        val args = RecordAudioFragmentArgs.fromBundle(requireArguments()).isFaceOrAudiRegistered
        if (args == Constants.FACE_NOT_REGISTERED) {
            promptRegisterFace()
        } else {
            navigateToPinFragment()
        }
    }

    private fun promptRegisterFace() {

        val prompt =
            VoiceInteractor.Prompt("Please register your face")

        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                navigateToFaceFragment()
                activity?.stopLocalVoiceInteraction()
            }
        })

    }

}