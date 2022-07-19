package com.example.mywallet.feature_wallet.presentation.auth.onboarding.otp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class OTPReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for(sms in messages){
           val message = sms.messageBody
        }
    }
}