package com.example.mywallet.feature_wallet.presentation.auth.onboarding.otp

import android.app.VoiceInteractor
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.util.Constants
import com.example.mywallet.databinding.FragmentVerifyOTPBinding
import com.example.mywallet.feature_wallet.data.remote.voiceRetrofit
import com.example.mywallet.feature_wallet.domain.model.User
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


class VerifyOTPFragment : Fragment(R.layout.fragment_verify_o_t_p), LoginVoiceInteraction {
    private val TAG = VerifyOTPFragment::class.java.simpleName
    lateinit var binding: com.example.mywallet.databinding.FragmentVerifyOTPBinding
    lateinit var mCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    lateinit var viewModel: OTPViewModel
    lateinit var voiceState: OtpVoiceState
    lateinit var mAuth: FirebaseAuth
    lateinit var timer: CountDownTimer
    private var verificationId: String? = null
    lateinit var inputCode1: EditText
    lateinit var inputCode2: EditText
    lateinit var inputCode3: EditText
    lateinit var inputCode4: EditText
    lateinit var inputCode5: EditText
    lateinit var inputCode6: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVerifyOTPBinding.bind(view)
        inputCode1 = binding.inputCode1
        inputCode2 = binding.inputCode2
        inputCode3 = binding.inputCode3
        inputCode4 = binding.inputCode4
        inputCode5 = binding.inputCode5
        inputCode6 = binding.inputCode6
        viewModel = OTPViewModel()

       viewModel.onEvent(OTPViewModel.OTPUiEvent.GetOTPPrompt)
        lifecycleScope.launchWhenStarted {
            subscribeUiState()
        }

        mAuth = FirebaseAuth.getInstance()
        initCallBack()
        initTimer()
        setUpOTPInputs()
        val otp = createOtp()
        val smsManager: SmsManager = SmsManager.getDefault()


        binding.btnGetOPT.setOnClickListener {
            timer.start()
//            if (inputCode1.text.trim().isEmpty() || inputCode2.text.trim().isEmpty() || inputCode3.text.trim().isEmpty() ||
//                inputCode4.text.trim().isEmpty() || inputCode5.text.trim().isEmpty() || inputCode6.text.trim().isEmpty()){
//                showToast("Please enter a valid code")
//                    return@setOnClickListener
//            }

            //sendVerificationCode("+2547040188825")
            smsManager.sendTextMessage(
                "07040188825", "ECLECTICS",
                "Otp for verification is: $otp", null, null
            )
        }


    }

    private suspend fun subscribeUiState() {
        viewModel.uiState.collectLatest { state ->
            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(Bundle())
            }
        }
    }


    private fun createOtp(): Int {
        return Random().nextInt(9) + Random().nextInt(9) +
                Random().nextInt(9) + Random().nextInt(9) +
                Random().nextInt(9) + Random().nextInt(9)
    }

    private fun setUpOTPInputs() {
        inputCode1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    inputCode2.requestFocus()
                } else {
                    return
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        inputCode2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    inputCode3.requestFocus()
                } else {
                    inputCode1.requestFocus()
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        inputCode3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    inputCode4.requestFocus()
                } else {
                    inputCode2.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        inputCode4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    inputCode5.requestFocus()
                } else {
                    inputCode3.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        inputCode5.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    inputCode6.requestFocus()
                } else {
                    inputCode4.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        inputCode6.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    return
                } else {
                    inputCode5.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }


    private fun initCallBack() {
        mCallBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {


            override fun onCodeSent(
                s: String,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(s, forceResendingToken)
                verificationId = s
            }


            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // below line is used for getting OTP code
                // which is sent in phone auth credentials.
                val code = phoneAuthCredential.smsCode
                // if the code is not null then
                // we are setting that code to
                // our OTP edittext field.
                if (code != null) {
                    Log.d(TAG, "onVerificationCompleted: Sms Code -> $code")
                    // after setting this code
                    // to OTP edittext field we
                    // are calling our verifycode method.
                    verifyCode(code);

                }


            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Log.d(TAG, "onVerificationFailed: ${p0.message}")
            }

        }
    }

    suspend fun getOtp() {
        binding.btnGetOPT.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        delay(5000L)
        binding.progressBar.visibility = View.GONE
        binding.btnGetOPT.visibility = View.VISIBLE

        binding.inputCode1.setText("4")
        binding.inputCode2.setText("2")
        binding.inputCode3.setText("8")
        binding.inputCode4.setText("9")
        binding.inputCode5.setText("5")
        binding.inputCode6.setText("0")
        viewModel.onEvent(OTPViewModel.OTPUiEvent.DeviceVerificationIsSuccessFull)
    }

    private fun verifyCode(code: String) {
        // below line is used for getting
        // credentials from our verification id and code
        val credential = verificationId?.let { PhoneAuthProvider.getCredential(it, code) }
        // after getting credential we are
        // calling sign in method.
        if (credential != null) {
            signInWithCredential(credential)
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        mAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Successful", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "onComplete: Exception -> ${task.exception?.message}")
            }
        }
    }


    private fun initTimer() {
        timer = object : CountDownTimer(60 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTimer.text = "Resend code in ${millisUntilFinished * 1000}s"
            }

            override fun onFinish() {
                binding.tvTimer.text = getString(R.string.request_a_new_code)
            }

        }
    }

    private fun sendVerificationCode(number: String) {
        // this method is used for getting
        // OTP on user phone number.
        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(mCallBack)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)


    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.getOtpMessagePrompt -> {
                    promptOtpMessage()
                }
                voiceState.verificationSuccessFull -> {
                    promptVerificationSuccessFull()
                }
            }
        }
    }

    private fun promptVerificationSuccessFull() {
        val prompt = VoiceInteractor.Prompt("Your Device is Verified successfully")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                    promptToEnableBiometrics()

            }
        })
    }

    private fun promptOtpMessage() {
        val prompt = VoiceInteractor.Prompt("Hello there, We are sending  an OTP to your number")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.stopLocalVoiceInteraction()
                lifecycleScope.launch {
                    getOtp()
                }

            }
        })
    }

    private fun promptToEnableBiometrics() {
        val prompt =
            VoiceInteractor.Prompt(
                "To make sure your banking is secure, " +
                        "you must enable one of the sign in biometrics, which one do you wish to enable Face or Voice authentication"
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

    private fun navigateToAudioFragment() {
        val action = VerifyOTPFragmentDirections.actionVerifyOTPFragmentToRecordAudioFragment(
            Constants.FACE_NOT_REGISTERED,
            User()
        )
        findNavController().navigate(action)

    }

    private fun navigateToFaceFragment() {
        val user = VerifyOTPFragmentArgs.fromBundle(requireArguments()).user
        val action = VerifyOTPFragmentDirections.actionVerifyOTPFragmentToFaceFragment(Constants.AUDIO_NOT_REGISTERED, user)
        findNavController().navigate(action)
    }
}