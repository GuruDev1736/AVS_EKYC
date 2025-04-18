package com.avs.avs_ekyc.Constant

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.datepicker.MaterialDatePicker
import es.dmoral.toasty.Toasty
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

object Constant {

    fun success(context: Context, message: String) {
        Toasty.success(context, message, Toast.LENGTH_SHORT, true).show();
    }

    fun error(context: Context, message: String) {
        Toasty.error(context, message, Toast.LENGTH_SHORT, true).show();
    }

    fun showDatePicker(context: Context, textView: EditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.format(Date(selection))
            textView.setText(selectedDate)
        }
    }

    fun callPhone(phoneNo: String, context: Context) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNo")
        }
        context.startActivity(intent)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBase64ExcelToDownloads(context: Context, base64String: String?, fileName: String) {
        if (base64String.isNullOrEmpty()) {
            Toast.makeText(context, "Base64 string is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val decodedBytes = Base64.getDecoder().decode(base64String)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, if (fileName.endsWith(".xlsx")) fileName else "$fileName.xlsx")
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val itemUri = resolver.insert(collection, contentValues)

        itemUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(decodedBytes)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(context, "Excel saved to Downloads", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
        }
    }
}