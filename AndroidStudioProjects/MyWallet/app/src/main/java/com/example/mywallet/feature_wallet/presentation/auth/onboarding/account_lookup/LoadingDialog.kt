package com.example.mywallet.feature_wallet.presentation.auth.onboarding.account_lookup

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.example.mywallet.R

class LoadingDialog(val activity: Activity) {

    lateinit var dialog: AlertDialog

    fun startLoadingDialog() {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        builder.setView(inflater.inflate(R.layout.loading_custom_dialog, null))
        builder.setCancelable(true)

        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

    }

    fun dismissDialog() {
        if (::dialog.isInitialized) {
            dialog.dismiss()
        }
    }
}