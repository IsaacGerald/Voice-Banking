package com.example.mywallet.feature_wallet.presentation.wallet.transaction

import android.app.VoiceInteractor
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mywallet.R
import com.example.mywallet.core.util.VoiceInteraction
import com.example.mywallet.databinding.FragmentTransactionBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.Transfer
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetCurrentBalance
import com.example.mywallet.feature_wallet.domain.use_case.GetTransactions
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import com.example.mywallet.feature_wallet.presentation.wallet.MainActivity
import com.example.mywallet.feature_wallet.presentation.ui.TransactionAdapter
import com.example.mywallet.feature_wallet.presentation.wallet.transaction.TransactionsViewModel.*
import kotlinx.coroutines.flow.collectLatest


class TransactionFragment : Fragment(R.layout.fragment_transaction), VoiceInteraction {
    private val TAG = TransactionFragment::class.java.simpleName
    lateinit var binding: FragmentTransactionBinding
    lateinit var transactionAdapter: TransactionAdapter
    lateinit var voiceState: TransVoiceState
    private var currentBalance: Int = 0

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi)
    }

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as MainActivity).dataStore)
    }
    private val viewModel by viewModels<TransactionsViewModel> {
        TransactionsViewModelFactory(
            this,
            GetTransactions(repository),
            GetCurrentBalance(userPreferences)
        )
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransactionBinding.bind(view)

        viewModel.onEvent(TransUiEvent.GetExtras(extras = arguments))

        initLayout()

        lifecycleScope.launchWhenStarted {
            subscribeTransactionsUiState()
        }


    }

    private fun initLayout() {
        transactionAdapter = TransactionAdapter()

        binding.RecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = transactionAdapter
        }
    }

    private suspend fun subscribeTransactionsUiState() {

        viewModel.transUiState.collectLatest { state ->

            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            state.transactions?.let {
                transactionAdapter.submitList(it)
            }

            state.currentBalance?.let {
                currentBalance = Integer.parseInt(it)
                binding.txtBalance.text = it
            }

            state.navigateToServiceTransfer?.let { transfer ->
                navigateToServiceTransfer(transfer)
            }
        }
    }

    private fun navigateToServiceTransfer(transfer: Transfer) {
        val action =
        TransactionFragmentDirections.actionTransactionFragmentToServiceTransferFragment()
        action.transferModel = transfer
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        logArgs(arguments)
    }


    private fun logArgs(bundle: Bundle?) {
        bundle?.let {
            Log.d(TAG, "======= logIntent ========= %s")
            Log.d(TAG, "Logging intent data start")

            bundle.keySet().forEach { key ->
                Log.d(TAG, "[$key=${bundle.get(key)}]")
            }

            Log.d(TAG, "Logging intent data complete")
        }

    }


    override fun isVoiceInteraction() {
        if (::voiceState.isInitialized){
          if (voiceState.isCurrentBalance){
              promptCurrentBalance()
          }
        }


    }

    private fun promptCurrentBalance() {
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(
                VoiceInteractor.Prompt("your current balance is $currentBalance"),
                Bundle()
            ) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.stopLocalVoiceInteraction()
            }
        })
    }


}