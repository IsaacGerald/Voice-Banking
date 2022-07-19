package com.example.mywallet.feature_wallet.presentation.auth.login.welcomescreen

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.audio.AudioRecorder
import com.example.mywallet.databinding.FragmentWelcomeLoginBinding
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
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginActivity
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginViewModel
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginViewModelFactory
import com.example.mywallet.feature_wallet.presentation.auth.login.VoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class WelcomeLoginFragment : Fragment(R.layout.fragment_welcome_login), VoiceInteraction {
    private val TAG = WelcomeLoginFragment::class.java.simpleName
    lateinit var binding: FragmentWelcomeLoginBinding
    lateinit var voiceState: WelcomeLoginVoiceState
    lateinit var recorder: AudioRecorder
    lateinit var progressDialog: AlertDialog

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi,
        )
    }

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as LoginActivity).dataStore)
    }

    private val viewModel by viewModels<WelcomeLoginViewModel> {
        WelcomeLoginViewModelFactory(
            this,
            GetIsNewUser(userPreferences),
            GetVoiceLogin(repository)

        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as LoginActivity).initVoiceInteraction(this)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWelcomeLoginBinding.bind(view)

        recorder = AudioRecorder(requireContext())

        binding.btnLogin.setOnClickListener {
            navigateToLoginFragment()
        }

        viewModel.onEvent(WelcomeLoginViewModel.UiEvent.GetIsNewUser)
        viewModel.onEvent(WelcomeLoginViewModel.UiEvent.GetWelcomePrompt)


        lifecycleScope.launch {
            subscribeUiState()
        }
    }

    private suspend fun subscribeUiState() {
        viewModel.uiState.collectLatest { state ->
            state.isNewUser?.let { isNewUser ->
                if (isNewUser) {
                    Log.d(TAG, "subscribeUiState: State -> $state")
                    Toast.makeText(context, "Is a new User", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "subscribeUiState: state -> $state")
                    Toast.makeText(context, "Is not a new User", Toast.LENGTH_SHORT).show()
                }
            }

            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            if (state.isLoading) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
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
        val action = WelcomeLoginFragmentDirections.actionWelcomeLoginFragmentToFaceLoginFragment()
        findNavController().navigate(action)
    }

    private fun navigateToOnBoarding() {
        Toast.makeText(context, "Is new User", Toast.LENGTH_SHORT).show()
        val onBoardingIntent =
            Intent((requireActivity() as LoginActivity), OnBoardingActivity::class.java)
        startActivity(onBoardingIntent)

    }

    private fun navigateToLoginFragment() {
        val action = WelcomeLoginFragmentDirections.actionWelcomeLoginFragmentToLoginFragment2()
        findNavController().navigate(action)
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

    private suspend fun startRecording() {
        recorder.startRecording(_internalStorage = false)

        delay(5000)
        val filePath = recorder.stopRecording()
        showToast("Stopped recording..")
        //val path = LoginFragmentArgs.fromBundle(requireArguments()).enrollmentVoice

        if (filePath != null) {
            //viewModel.validateUserVoice(filePath)
            viewModel.onEvent(WelcomeLoginViewModel.UiEvent.ValidateUsersVoice(filePath))
        }

        //viewModel.setLoginUIEvent(LoginEvent.ShowProgressBar)
    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.welcomePrompt -> {
                    welcomePrompt()
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
                            activity?.finish()
                        }
                    }
                }
            }
        })
    }


}