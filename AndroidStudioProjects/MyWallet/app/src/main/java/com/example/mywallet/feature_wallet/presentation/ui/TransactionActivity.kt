package com.example.mywallet.feature_wallet.presentation.ui

import android.app.VoiceInteractor
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mywallet.R
import com.example.mywallet.databinding.ActivityTransferBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.USER_PREFERENCES_NAME
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransactionActivity : AppCompatActivity() {
    private val TAG = TransactionActivity::class.java.simpleName
    lateinit var transactionAdapter: TransactionAdapter
    lateinit var binding: ActivityTransferBinding
    lateinit var userPreferences: UserPreferences
    private var balance: String? = null
    private var showCurrentBalance = false
    private var helperPrompt = false

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_transfer)
        transactionAdapter = TransactionAdapter()
        userPreferences = UserPreferences(dataStore);
        binding.RecyclerView.apply {
            adapter = transactionAdapter
            layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        }


        getCurrentBalance()
        Log.d(TAG, "onViewCreated: isVoiceInteraction -> $isVoiceInteraction")

        getTransactions()

        helperPrompt = true
        startLocalVoiceInteraction(Bundle())


//        if (intent?.extras != null) {
//            intent?.handleIntent()
//            logIntent(intent)
//        }



    }


    private fun getCurrentBalance() {
        lifecycleScope.launch {
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@TransactionActivity, Observer { amount ->
                    val newAmount = "%,d".format(amount)
                    balance = newAmount
                    binding.txtBalance.text = newAmount
                    viewModel.setCurrentValue(amount)
                })
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_clearTransactions -> viewModel.clearTransactions()
            R.id.action_logout -> logout()
            R.id.action_resetAccount -> resetAccountBalance()
            else -> return super.onOptionsItemSelected(item)
        }
        return true

    }

    private fun resetAccountBalance() {
        lifecycleScope.launch {
            userPreferences.saveCurrentBalance(250000)
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@TransactionActivity, Observer { currentBalance ->

                    currentBalance?.let { balance ->
                        val newAmount = "%,d".format(balance)
                        binding.txtBalance.text = newAmount
                    }

                })
            userPreferences.updateValidUser(false)
        }
    }


    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()
        showToast("onLocalVoiceInteractionStarted: $isVoiceInteraction")

        if (isVoiceInteraction) {
            if (showCurrentBalance) {
                currentBalancePrompt()
            }
            if (helperPrompt){
                helpPrompt()
            }
        }

    }

    private fun helpPrompt() {
        val name = intent.getStringExtra("username")
        this.voiceInteractor.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(
                VoiceInteractor.Prompt("How may I help you?"),
                Bundle()
            ) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                helperPrompt = false
            }
        })
    }

    private fun currentBalancePrompt() {
        this.voiceInteractor.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(
                VoiceInteractor.Prompt("your current balance is $balance"),
                Bundle()
            ) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                showToast("onLocalVoiceInteractionStarted: $isVoiceInteraction")
                val extra = result?.get("balance")
                showToast("Result is -> $extra")
                Log.d(TAG, "onCompleteResult: Request completed!")
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: called...")
        intent?.handleIntent()
    }

    private fun Intent.handleIntent() {
        when (action) {
            Intent.ACTION_VIEW -> {
                if (isVoiceInteraction) {
                    Log.d(TAG, "handleIntent: VoiceInteraction started")
                } else {
                    Log.d(TAG, "handleIntent: VoiceInteraction not started")
                }
                val feature = intent?.extras?.getString("feature")
                val thing = intent?.extras?.getString("name")
                val invoice = intent?.extras?.getString("forServiceName")
                val transfer = intent?.extras?.getString("transferMode")


                when {
                    feature != null -> {
                        showCurrentBalance = true
                        Log.d(TAG, "handleIntent: feature -> $feature")
                        val bundle = Bundle()
                        bundle.putString("balance", feature)
                        startLocalVoiceInteraction(bundle)
                        startLocalVoiceInteraction(bundle)
                        startLocalVoiceInteraction(bundle)
                        viewModel.showBalance(true)
                        getBalance()
                    }
                    thing != null -> {
                        Log.d(TAG, "handleIntent: thing -> $thing")
                    }
                    invoice != null -> {
                        Log.d(TAG, "handleIntent: invoice -> $invoice")
                    }
                    transfer != null -> {
                        Log.d(TAG, "handleIntent: Transfer")
                        viewModel.setTransferData(intent.extras)
                        saveTransfers(intent.extras)


                    }
                }


            }
        }
    }

    private fun saveTransfers(extras: Bundle?) {
        extras.let {
            val originName: String? = it?.getString("moneyTransferOriginName")
            val destinationName = it?.getString("moneyTransferDestinationName")
            val value: String? = it?.getString("value")
            val currency = it?.getString("currency")
            val originProviderName = it?.getString("moneyTransferOriginProvidername")
            val originDestinationName = it?.getString("moneyTransferDestinationProvidername")
            val transfer: String? = it?.getString("transferMode")
            lifecycleScope.launch {
                userPreferences.saveTransfer(
                    originName,
                    destinationName,
                    value,
                    currency,
                    originProviderName,
                    originDestinationName,
                    transfer!!
                )
            }
        }

        moveToTransferActivity(intent.extras)
    }

    private fun logIntent(intent: Intent?) {
        val bundle: Bundle = intent?.extras ?: return

        Log.d(TAG, "======= logIntent ========= %s")
        Log.d(TAG, "Logging intent data start")
        Log.d(TAG, "logIntent: Action -> ${intent.action}")

        bundle.keySet().forEach { key ->
            Log.d(TAG, "[$key=${bundle.get(key)}]")
        }

        Log.d(TAG, "Logging intent data complete")
    }

    private fun moveToTransferActivity(extras: Bundle?) {
        Log.d(TAG, "moveToTransferActivity: $extras")
        val intent = Intent(this, ServiceTransferActivity::class.java)
        if (extras != null) {
            intent.putExtras(extras)
            startActivity(intent)
            this.intent = null
        }


    }

    private fun getBalance() {

        lifecycleScope.launch {
            userPreferences.getCurrentBalance().asLiveData()
                .observe(this@TransactionActivity, Observer { amount ->
                    val currentBalance = "%,d".format(amount)
                    //val prompt = getPrompt(currentBalance)
                    if (isVoiceInteraction) {
                        // submitRequest(prompt)
                    }
                    // showToast("Your current balance is $currentBalance ")
                })
        }


    }


    private fun getTransactions() {
        lifecycleScope.launch {
            viewModel.transaction.observe(this@TransactionActivity, Observer { transactions ->
                if (!transactions.isNullOrEmpty()) {
                    transactionAdapter.submitList(transactions)
                }
            })
        }
    }

    private fun getPrompt(prompt: String): VoiceInteractor.Prompt {
        return VoiceInteractor.Prompt(prompt)
    }


    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }

    private fun logout(): Boolean {
        lifecycleScope.launch {
            userPreferences.updateLoggInstate(false)
        }

        return true
    }

    override fun onDestroy() {
        stopLocalVoiceInteraction()
        super.onDestroy()
    }
}

class CompleteRequest(
    val prompt: VoiceInteractor.Prompt,
    val bundle: Bundle?,
) : VoiceInteractor.CompleteVoiceRequest(prompt, bundle) {
    override fun onCompleteResult(result: Bundle?) {
        super.onCompleteResult(result)
        Toast.makeText(context, "Complete request", Toast.LENGTH_SHORT).show()
    }
}

