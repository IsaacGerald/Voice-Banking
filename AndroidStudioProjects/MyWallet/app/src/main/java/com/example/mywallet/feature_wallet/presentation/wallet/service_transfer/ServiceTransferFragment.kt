package com.example.mywallet.feature_wallet.presentation.wallet.service_transfer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mywallet.R
import com.example.mywallet.core.util.VoiceInteraction
import com.example.mywallet.databinding.FragmentTransferBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.presentation.wallet.MainActivity
import kotlinx.coroutines.flow.collectLatest


class ServiceTransferFragment : Fragment(R.layout.fragment_transfer), VoiceInteraction {
    lateinit var binding: FragmentTransferBinding
    lateinit var voiceState: STVoiceState
    private var amount: Int = 0
    private var source: String? = null
    private val TAG = ServiceTransferFragment::class.java.simpleName

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi
        )
    }

    private val viewModel by viewModels<ServiceTransferViewModel> {
        ServiceTransferViewModelFactory(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity() as MainActivity).initVoiceInteraction(this)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransferBinding.bind(view)

            val transferModel =
                ServiceTransferFragmentArgs.fromBundle(requireArguments()).transferModel
        if (transferModel != null) {
            viewModel.setArgs(transferModel)
        }


        lifecycleScope.launchWhenStarted {
            subscribeTransactionUiState()
        }
    }

    private suspend fun subscribeTransactionUiState() {
        viewModel.serviceUiState.collectLatest { state ->
            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            state.amount?.let {
                amount = it
            }

            state.source?.let {
                source = it
            }



        }

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
        if (::voiceState.isInitialized) {
            when {
                voiceState.validateReceiveAmount -> {
                    showToast("How much do you want to request")
                }
                voiceState.validateSourceOfTransfer -> {
                    showToast("Where do you want to request from")
                }
                voiceState.confirmReceiveTransfer -> {
                    showToast("Receiving 100 for account ending with ..657 do you wish to proceed")
                }
            }
        }
    }

    private fun showToast(toastMsg: String) {
        Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
    }


}