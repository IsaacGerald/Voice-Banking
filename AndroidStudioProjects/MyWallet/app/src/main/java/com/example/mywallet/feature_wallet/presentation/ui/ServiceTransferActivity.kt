package com.example.mywallet.feature_wallet.presentation.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.example.mywallet.R
import com.example.mywallet.core.util.Constants
import com.example.mywallet.core.util.Constants.RECEIVE_MONEY
import com.example.mywallet.core.util.Constants.SEND_MONEY
import com.example.mywallet.databinding.ActivityServiceTransferBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.Transaction
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ServiceTransferActivity : AppCompatActivity() {
    private val TAG = ServiceTransferActivity::class.java.simpleName
    lateinit var binding: ActivityServiceTransferBinding
    private var recipientName: String? = ""
    private var sendAmount: String? = ""
    private var currentBalance: Int = 0
    private var amountReceived:Int = 0
    private var source: String? = null
    private var isTransactionCompleted = false
    private var isTransactionValidated = false
    private var confirmTransferRecipient = false
    private var validateSource = false
    private var validateValue = false
    private lateinit var model: FaceModel
    lateinit var userPreferences: UserPreferences
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var speechRecognizerIntent: Intent
    private var isUserIdFocused = false
    private var showDialogBox = false
    private var verificationStatus = false
    private var isUserPasswordFocused = false
    lateinit var tts: TextToSpeech
    private var path: String? = null
    private var isValidUser = true
    private var verifyUser = false
    private var timer = 50
    private var transferMode: String? = null
    private var captureUser = false
    private var recipientIsfocused = false
    lateinit var dialogEditText: EditText
    lateinit var loadingDialog: AlertDialog
    lateinit var builder: AlertDialog.Builder

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(this).transactionDao(),
            TransactionDatabase.getDatabase(this).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi,
        )
    }
    private val viewModel by viewModels<TransactionViewModel> {
        TransactionViewModelFactory(this, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startLocalVoiceInteraction(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_service_transfer)
        userPreferences = UserPreferences(dataStore)
        viewModel.setTransferData(intent.extras)
        model = FaceModel()

        logBundle()
        getCurrentBalance()
        Log.d(TAG, "onCreate: Service activity")

        binding.btnReceive.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("camera", "Camera")
            startActivity(intent)
        }

        binding.btnSend.setOnClickListener {
            sendMoney()
        }




        handleTransfer()
        //readContacts()
        initSpeechRecognizer()

    }

    private fun logBundle() {
        Log.d(TAG, "======= logIntent ========= %s")
        Log.d(TAG, "Logging intent data start")
        val bundle = viewModel.transfer.value
        bundle?.keySet()?.forEach { key ->
            Log.d(TAG, "[$key=${bundle.get(key)}]")
        }
    }


    override fun onResume() {
        super.onResume()
        val bundle: Bundle? = intent?.extras
        bundle?.keySet()?.forEach { key ->
            Log.d(TAG, "[$key=${bundle.get(key)}]")
        }

        lifecycleScope.launch {
            userPreferences.getTransfers().collect{ transfer ->

                source = transfer.originName
                transferMode = transfer.transferMode
                if (transfer.value != ""){
                    amountReceived = Integer.parseInt(transfer.value!!)
                }

            }
        }

        handleTransfer()

    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
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
                if (results != null) {
                    //stopLocalVoiceInteraction()
                    validateValue = false
                    speechRecognizer.stopListening()
                    timer = 0
                    val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "onResults: SpeechRecognizer -> ${data!![0]}")
                    amountReceived = Integer.parseInt(data[0]);

                    when (path) {
                        Constants.RECEIVE_MONEY -> {
                            if (binding.ediTextReceiveAmount.isFocused) {
                                binding.ediTextReceiveAmount.setText(data[0])
                                binding.ediTextReceiveAmount.clearFocus()
                                validateReceiveTransfer()
                            } else {
                                dialogEditText.setText(data[0])
                                isTransactionValidated = true
                                verifyPin()
                            }

                        }
                        SEND_MONEY -> {
                            if (binding.ediTextSendTo.isFocused) {
                                binding.ediTextSendTo.clearFocus()
                                Log.d(TAG, "onResults:  binding to recipient")
                                binding.ediTextSendTo.setText(data[0])
                                recipientName = data[0]
                                validateSendTransfer()
                            } else {
                                Log.d(TAG, "onResults:  binding to amount")
                                binding.ediTextAmount.setText(data[0])
                                validateSendTransfer()
                            }

                        }
                    }

                }
            }

            override fun onPartialResults(partialResults: Bundle?) {

            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }
        })
    }

    private fun verifyPin() {
        dismissDialog()
        startLocalVoiceInteraction(Bundle())
    }

    private fun handleTransfer() {
       // val bundle = viewModel.transfer.value
        //val transfer: String? = bundle?.getString("transferMode")
        transferMode?.let {
            Log.d(TAG, "handleIntent: transfer -> ${Uri.parse(it)}")
            val uri = Uri.parse(it)
            path = uri.path
            Log.d(TAG, "handleIntent: Path -> $path")
            handleTransfer(path)
        }



    }


    private fun handleTransfer(path: String?) {
        if (path == null) {
            return
        }

        val bundle = viewModel.transfer.value

        // val originName = intent?.extras?.getString("moneyTransferOriginName")
        val originName: String? = bundle?.getString("moneyTransferOriginName")
        source = originName
        val destinationName =
            bundle?.getString("moneyTransferDestinationName")
        recipientName = destinationName
        val value: String? = bundle?.getString("value")
        val currency = bundle?.getString("currency")
        val originProviderName =
            bundle?.getString("moneyTransferOriginProvidername")
        val originDestinationName =
            bundle?.getString("moneyTransferDestinationProvidername")
       // val bndle = intent.extras?.getBoolean("isValidated")
       // showToast(bndle.toString())


        showToast("Service Activity")
        when (path) {
            "/SendMoney" -> {
                Log.d(TAG, "handleTransfer: -> sending money...")
                handleSendMoney(
                    originName,
                    destinationName,
                    value,
                    currency,
                    originProviderName,
                    originDestinationName
                )

            }
            "/ReceiveMoney" -> {
                Log.d(TAG, "handleTransfer: -> Receiving money...")
                handleReceiveMoney(
                    originName,
                    destinationName,
                    value,
                    currency,
                    originProviderName,
                    originDestinationName
                )
            }

        }
    }

    private fun handleReceiveMoney(
        originName: String?,
        destinationName: String?,
        value: String?,
        currency: String?,
        originProviderName: String?,
        originDestinationName: String?
    ) {

        lifecycleScope.launch {
            userPreferences.getIsUserValid().collect{isValid ->
                isValidUser = isValid
            }
        }

            if (isValidUser) {
//                if (value != null) {
//                    amountReceived = value
//                } else if (originName != null) {
//                    Log.d(TAG, "handleReceiveMoney: Origin name -> $originName")
//                    source = originName
//                }
                Log.d(TAG, "handleReceiveMoney: Origin name -> $originName")
                Log.d(TAG, "handleReceiveMoney: source -> $source")

                showToast("User is valid")

                if (!isValidOriginName(originName) || !isValidValue(value)) {
                    if (intent.extras != null) {
                        startLocalVoiceInteraction(Bundle())
                    }
                } else {
                    validateReceiveTransfer()

                }
            } else {
                verifyUser = true
                startLocalVoiceInteraction(Bundle())
                return
            }




        showToast("Contunue")

    }

    private fun handleSendMoney(
        originName: String?,
        destinationName: String?,
        value: String?,
        currency: String?,
        originProviderName: String?,
        originDestinationName: String?
    ) {

//        if (!model.status) {
//            verifyUser = true
//            startLocalVoiceInteraction(Bundle())
//            return
//        }

        if (value != null) {
            sendAmount = value
            binding.ediTextAmount.setText(value)
        } else if (destinationName != null) {
            binding.ediTextSendTo.setText(destinationName)
            recipientName = destinationName
        }


        if (!isValidDestinationName(destinationName) || !isValidValue(value)) {
            if (intent.extras != null) {
                startLocalVoiceInteraction(Bundle())
            }
            showToast("Incomplete")
        } else {
            showToast("Complete transfer")
            validateSendTransfer()
        }


    }

    private fun getCurrentBalance() {
        lifecycleScope.launch {
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@ServiceTransferActivity, Observer {
                    viewModel.setCurrentValue(it)
                    currentBalance = it

                })
        }

    }

    private fun sendMoney() {
        val recipient = binding.ediTextSendTo.text.toString()
        val amount = binding.ediTextAmount.text.toString()

        validateSendTransfer()


    }

    private fun validateReceiveTransfer() {
        if (!validateAmountReceived() || !validateSource()) {
            if (intent.extras != null) {
                startLocalVoiceInteraction(Bundle())
            } else {
                return
            }

        } else {
            Log.d(TAG, "validateReceiveTransfer: Receive Transaction is valid..")
            isTransactionValidated = true
            // showDialogBox = true
            if (intent.extras != null) {
                Log.d(TAG, "validateReceiveTransfer: starting local interaction")

                startLocalVoiceInteraction(Bundle())
            } else {
                completeReceiveTransaction()
            }
        }
    }

    private fun completeReceiveTransaction() {
        // val balance = currentBalance + Integer.parseInt(amountReceived)
        lifecycleScope.launch {
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@ServiceTransferActivity, Observer { amount ->
                    Log.d(TAG, "completeReceiveTransaction: -> $amountReceived")
                         val balance = amount + amountReceived
                         updateCurrentBalance(balance)

                    isTransactionCompleted = true

                })
        }

        val transaction = Transaction(
            transactionName = source!!,
            amount = amountReceived,
            date = getCurrentDate(),
            type = "Receive"
        )
        viewModel.insertTransaction(transaction)
        showToast("Transaction successful")
        startLocalVoiceInteraction(Bundle())
        moveToTransactions()

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

    private fun validateSendTransfer() {

        if (!validateAmount() || !validateRecipient()) {
            if (intent.extras != null) {
                startLocalVoiceInteraction(Bundle())
            } else {
                return
            }
        } else {
            isTransactionValidated = true
            if (intent?.extras != null) {
                Log.d(TAG, "validateReceiveTransfer: starting local interaction")
                startLocalVoiceInteraction(Bundle())
            } else {
                completeSendTransaction()
            }

        }


    }


    private fun completeSendTransaction() {
        lifecycleScope.launch {
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@ServiceTransferActivity, Observer { amount ->
                    val newBalance = amount - Integer.parseInt(sendAmount!!)
                    updateCurrentBalance(newBalance)
                    // isTransactionValidated = false
                    isTransactionCompleted = true
                })
        }

        val amount = Integer.parseInt(sendAmount)
        val transaction = Transaction(
            transactionName = recipientName!!,
            amount = amount,
            type = "Sent",
            date = getCurrentDate()
        )

        viewModel.insertTransaction(transaction)
        showToast("Transaction successful!")
        startLocalVoiceInteraction(Bundle())
        moveToTransactions()

    }

    private fun validateSource(): Boolean {
        return if (!source.isNullOrEmpty()) {
            true
        } else {
            validateSource = true
            //startLocalVoiceInteraction(Bundle())
            showToast("Please enter the source of transfer")
            false
        }

    }

    private fun validateAmountReceived(): Boolean {
        // amountReceived = binding.ediTextReceiveAmount.text?.toString() ?: intent?.extras?.getString("moneyTransferOriginName")

        return if (amountReceived > 0) {
            // binding.ediTextReceiveAmount.setText(amountReceived)
            true
        } else {
            validateValue = true
            startLocalVoiceInteraction(Bundle())
            showToast("Please enter the amount")
            false
        }
    }

    private fun updateCurrentBalance(newBalance: Int) {
        lifecycleScope.launch {
            userPreferences.saveCurrentBalance(newBalance)
        }
    }

    private fun validateAmount(): Boolean {

        Log.d(TAG, "validateAmount: Current Amount -> $currentBalance")
        sendAmount = binding.ediTextAmount.text.toString()

        return if (sendAmount != "") {
            if (Integer.parseInt(sendAmount!!) >= 50000) {
                showToast("You balance is low to complete your transaction")
                false
            } else {
                true
            }

        } else {
            if (intent?.extras != null) {
                validateValue = true
            }
            showToast("Please enter the amount you wish to send")
            false
        }


    }


    private fun validateRecipient(): Boolean {
        // recipientName = binding.ediTextSendTo.text.toString()
        return if (recipientName != "") {
            true
        } else {
            confirmTransferRecipient = true
            showToast("Please enter the recipient")
            false
        }

    }

    private fun isValidDestinationName(destinationName: String?): Boolean {
        return if (destinationName == null) {
            if (intent.extras != null) {
                confirmTransferRecipient = true
            }
            showToast("Where do you want to send the money to?")
            false
        } else {
            binding.ediTextSendTo.setText(destinationName)
            viewModel.setDestinationName(destinationName)
            true
        }
    }


    private fun isValidOriginName(originName: String?): Boolean {
        return if (source.isNullOrEmpty()) {
            validateSource = true
            showToast("Which account do you want to send from")
            false
        } else {
            binding.ediTextReceive.setText(originName)
            viewModel.setOriginName(source!!)
            true
        }
    }

    private fun isValidValue(value: String?): Boolean {
        return if (amountReceived <= 0) {
            if (intent.extras != null) {
                validateValue = true
            }
            // showToast("How much do yo wish to Receive?")
            false
        } else {
            when (path) {
                RECEIVE_MONEY -> binding.ediTextReceiveAmount.setText(amountReceived.toString())
                SEND_MONEY -> binding.ediTextAmount.setText(amountReceived.toString())
            }

            viewModel.setValue(amountReceived!!.toLong())
            Log.d(TAG, "isValidValue: -> $value")
            true
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(Date())
    }

    private fun deposit() {
        var amount = 0
        val txt: String? = binding.ediTextReceive.text.toString()
        if (txt != null) {
            val depositAmount = Integer.parseInt(txt)
            lifecycleScope.launch {
                userPreferences.getCurrentBalance().asLiveData()
                    .observe(this@ServiceTransferActivity, Observer { currentBalance ->
                        amount = currentBalance
                    })
            }

            val currentBalance = amount + depositAmount
            saveBalance(currentBalance)
            updateTransaction()
        }

    }

    private fun saveBalance(currentBalance: Int) {
        lifecycleScope.launch {
            userPreferences.saveCurrentBalance(currentBalance)
            showToast("Deposit successful")
            moveToTransactions()
        }
    }

    private fun moveToTransactions() {
        if (intent?.extras != null) {
            intent = null
            startLocalVoiceInteraction(Bundle())
        }
        intent = null
        val intent = Intent(this, TransactionActivity::class.java)
       // viewModel.clearTransferData()
        lifecycleScope.launch {
            userPreferences.clearTransfers()
        }

        startActivity(intent)

    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateTransaction() {

    }

    private fun showDialog() {
        val dialogLayout =
            LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null)
        dialogEditText = dialogLayout.findViewById<EditText>(R.id.et_pin)
        this.loadingDialog = AlertDialog
            .Builder(this)
            .setView(dialogLayout)
            .setTitle("Enter your pin")
            .setPositiveButton(
                "Ok"
            ) { dialog, which -> val pin = dialogEditText.text.toString() }
            .setNegativeButton("Cancel") { dialog, which ->

            }
            .create()

        loadingDialog.show()

    }

    private fun dismissDialog() {
        if (::loadingDialog.isInitialized) {
            loadingDialog.dismiss()
        }
    }


    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()

        Log.d(TAG, "onLocalVoiceInteractionStarted: $isVoiceInteraction")

        if (isTransactionCompleted) {
            completeTransaction()
            //completeTrans()
        }

        if (isTransactionValidated) {
            validateTransaction()
        }

        if (confirmTransferRecipient) {
            //confirmRecipient()
            confirmTransRecipient()
        }

        if (verifyUser) {
            userVerification()
        }

        if (captureUser) {
            captureUserForVerification()
        }

        if (validateSource) {
            validateSourceOfTransaction()
        }


        if (validateValue) {
            validateValueForTransaction()
            //validateTranValue()
        }

    }

    private fun confirmTransRecipient() {
        val prompt =
            VoiceInteractor.Prompt(" Where do you want to send the money to? Local Banks, International bank or M-pesa")

        val option1 = VoiceInteractor.PickOptionRequest.Option("Local Bank", 1)
        option1.addSynonym("local")

        val option2 = VoiceInteractor.PickOptionRequest.Option("International bank", 1)
        option2.addSynonym("international")

        val option3 = VoiceInteractor.PickOptionRequest.Option("M-pesa", 1)
        option3.addSynonym("to M-pesa")


        voiceInteractor.submitRequest(object :
            VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1, option2, option3), null) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    val index = selections[0].index
                    binding.ediTextSendTo.setText(selections[0].label)
                    if (index == 2) {
                        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(
                            VoiceInteractor.Prompt(" Where do you want to send the money to?"),
                            Bundle()
                        ) {
                            override fun onCompleteResult(
                                result
                                : Bundle?
                            ) {
                                super.onCompleteResult(result)
                                lifecycleScope.launch {
                                    startListening()
                                }
                                binding.ediTextSendTo.requestFocus()
                                confirmTransferRecipient = false
                                stopLocalVoiceInteraction()

                            }
                        })
                    } else {
                        confirmTransferRecipient = false
                        stopLocalVoiceInteraction()
                    }
                }
            }
        })
    }

    private fun completeTrans() {
        val prompt = getPrompt("Your transaction is completed successfully")
        voiceInteractor.submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
            }
        })

        moveToTransactions()
    }

    private fun promptForPin() {
        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(
            VoiceInteractor.Prompt(
                "Please enter your Pin"
            ), null
        ) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                showDialog()
                timer = 50
                lifecycleScope.launch {
                    startListening()
                }
                dialogEditText.requestFocus()
                showDialogBox = false
                stopLocalVoiceInteraction()

            }
        })
    }

    private fun validateValueForTransaction() {
        // val prompt = getPrompt("How much do you want to request")
        showToast("Is local voice interaction suported: $isLocalVoiceInteractionSupported")
        var valuePrompt: VoiceInteractor.Prompt? = VoiceInteractor.Prompt("")
        if (path == "/SendMoney") {
            valuePrompt =
                VoiceInteractor.Prompt("How much do you want to send")
        } else if (path == "/ReceiveMoney") {
            valuePrompt =
                getPrompt("How much do you want to request")
        }
        val bundle = Bundle()
        bundle.putString("success", "success")

        voiceInteractor.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(
                valuePrompt,
                bundle
            ) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                Log.d(TAG, "onCompleteResult: onComplete called")
                timer = 50
                lifecycleScope.launch {
                    startListening()
                }

                when (path) {
                    "/SendMoney" -> binding.ediTextAmount.requestFocus()
                    "/ReceiveMoney" -> binding.ediTextReceiveAmount.requestFocus()
                }
                validateValue = false
                stopLocalVoiceInteraction()


            }

            override fun onCancel() {
                super.onCancel()
                //stopLocalVoiceInteraction()
            }
        })
    }

    private fun validateTranValue() {
        var valuePrompt: VoiceInteractor.Prompt? = VoiceInteractor.Prompt("")
        if (path == "/SendMoney") {
            valuePrompt =
                VoiceInteractor.Prompt("How much do you want to send, the minimum amount allowed is 100")
        } else if (path == "/ReceiveMoney") {
            valuePrompt =
                getPrompt("How much do you want to request, the minimum amount allowed is 50")
        }


        voiceInteractor.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(valuePrompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                showToast("Result is ${result?.isEmpty}")
                Log.d(TAG, "onConfirmationResult: $confirmed")
                if (confirmed) {
                }
                if (result?.isEmpty == true) {
                    Log.d(TAG, "onCompleteResult: onComplete called")
                    timer = 50
                    lifecycleScope.launch {
                        startListening()
                    }

                    when (path) {
                        "/SendMoney" -> binding.ediTextAmount.requestFocus()
                        "/ReceiveMoney" -> binding.ediTextReceiveAmount.requestFocus()
                    }
                    validateValue = false
                    stopLocalVoiceInteraction()
                }
            }
        })
    }

    private fun validateSourceOfTransaction() {
        Log.d(TAG, "onLocalVoiceInteractionStarted: Validating source...")
        // val prompt = getPrompt("From which account do want  to request the money")
        val option1 = VoiceInteractor.PickOptionRequest.Option("3-5-7", 1)
        option1.addSynonym("account ending with 357")

        val option2 = VoiceInteractor.PickOptionRequest.Option("2-4-8", 1)
        option2.addSynonym("account ending with 248")

        val option3 = VoiceInteractor.PickOptionRequest.Option("4-6-8", 1)
          option3.addSynonym("account ending with 468")



        voiceInteractor.submitRequest(object : VoiceInteractor.PickOptionRequest(
            VoiceInteractor.Prompt("From which account do want  to request the money, account ending with 3-5-7, account ending with 2-4-8 or account ending with 4-4-8"),
            arrayOf(option1, option2, option3),
            null
        ) {
            override fun onPickOptionResult(
                finished: Boolean,
                selections: Array<out Option>?,
                result: Bundle?
            ) {
                super.onPickOptionResult(finished, selections, result)
                if (finished && selections?.size == 1) {
                    binding.ediTextReceive.setText(selections[0].label)
                    source = selections[0].label.toString()
                    val index = selections[0].index
                    Log.d(TAG, "onPickOptionResult: ${selections[0].label}")
                    //isTransactionValidated = true
                    validateSource = false
                    stopLocalVoiceInteraction()
                    validateReceiveTransfer()


                }
            }
        })
    }

    private fun confirmRecipient() {
        Log.d(TAG, "onLocalVoiceInteractionStarted: Confirming transfer recipient")
        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(
            VoiceInteractor.Prompt(" Where do you want to send the money to?"),
            Bundle()
        ) {
            override fun onCompleteResult(
                result
                : Bundle?
            ) {
                super.onCompleteResult(result)
                lifecycleScope.launch {
                    startListening()
                }
                binding.ediTextSendTo.requestFocus()
                confirmTransferRecipient = false
                stopLocalVoiceInteraction()

            }
        })
    }

    private fun validateTransaction() {
        Log.d(TAG, "onLocalVoiceInteractionStarted: Confirming transaction completion")
        //"Receiving $amountReceived from $source, do you wish to complete this transaction?"
        var prompt: VoiceInteractor.Prompt? = VoiceInteractor.Prompt(" ")
        if (path == "/SendMoney") {
            prompt =
                VoiceInteractor.Prompt("Sending $sendAmount shillings to $recipientName, do you want to complete this transaction?")
        } else if (path == "/ReceiveMoney") {
            prompt =
                VoiceInteractor.Prompt("Receiving $amountReceived shillings from  account ending with $source , do you want to complete this transaction?")
        }

        voiceInteractor.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    isTransactionValidated = false
                    isTransactionCompleted = true

                    when (path) {
                        RECEIVE_MONEY -> {
                            stopLocalVoiceInteraction()
                            completeReceiveTransaction()
                        }
                        SEND_MONEY -> {
                            stopLocalVoiceInteraction()
                            completeSendTransaction()

                        }
                    }


                } else {

                }

            }
        })
    }

    private fun completeTransaction() {
        Log.d(TAG, "onLocalVoiceInteractionStarted: showing current balance...")
        val prompt = getPrompt("Your transaction is completed successfully")
        voiceInteractor.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {

            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                Log.d(TAG, "onCompleteResult: Request completed!")
                isTransactionCompleted = false
                stopLocalVoiceInteraction()
            }
        })
    }

    private fun captureUserForVerification() {
        val prompt = VoiceInteractor.Prompt("Opening camera")
        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                moveToCameraActivity()
            }
        })
    }

    private fun moveToCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        val bundle = viewModel.transfer.value
        Log.d(TAG, "moveToCameraActivity: ${bundle?.get("transferMode")}")
        intent.putExtra("extra", intent.extras)
        startActivity(intent)
    }

    private fun userVerification() {
        val prompt = VoiceInteractor.Prompt(
            "We first need to verify you before making the transaction"
        )
//        voiceInteractor.submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
//            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
//                super.onConfirmationResult(confirmed, result)
//                if (confirmed) {
//                    moveToCameraActivity()
//                } else {
//                    showToast("Cannot proceed with the transaction")
//                }
//                verifyUser = false
//                stopLocalVoiceInteraction()
//            }
//        })

        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(prompt, null){
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                verifyUser = false
                stopLocalVoiceInteraction()
                moveToCameraActivity()
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


    private fun getPrompt(prompt: String): VoiceInteractor.Prompt? {
        return VoiceInteractor.Prompt(prompt)
    }


}