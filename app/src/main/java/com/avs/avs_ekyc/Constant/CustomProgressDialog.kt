package com.avs.avs_ekyc.Constant

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.avs.avs_ekyc.R
import kotlin.apply


class CustomProgressDialog(private val context: Context) {
    private var dialog: Dialog? = null

    fun show(message: String = "Loading...") {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            val view = LayoutInflater.from(context).inflate(R.layout.custom_progress_dialog, null)
            setContentView(view)
            val progressText: TextView = view.findViewById(R.id.progress_text)
            progressText.text = message
            window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        }
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}