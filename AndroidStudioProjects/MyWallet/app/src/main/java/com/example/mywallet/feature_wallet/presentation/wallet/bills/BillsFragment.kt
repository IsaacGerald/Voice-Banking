package com.example.mywallet.feature_wallet.presentation.wallet.bills

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.example.mywallet.R
import com.example.mywallet.core.util.VoiceInteraction
import com.example.mywallet.databinding.FragmentBillsBinding
import com.example.mywallet.feature_wallet.presentation.wallet.MainActivity


class BillsFragment : Fragment(R.layout.fragment_bills), VoiceInteraction {
    lateinit var binding: FragmentBillsBinding
    lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = requireActivity() as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBillsBinding.bind(view)
    }

    override fun isVoiceInteraction() {

    }


}