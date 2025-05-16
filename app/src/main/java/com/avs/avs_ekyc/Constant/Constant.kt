package com.avs.avs_ekyc.Constant

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.datepicker.MaterialDatePicker
import es.dmoral.toasty.Toasty
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveJsonToDownloads(context: Context, jsonObject: JSONObject, fileName: String = "request.json"): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val documentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileUri = resolver.insert(documentUri, contentValues)

        if (fileUri != null) {
            resolver.openOutputStream(fileUri)?.use { outputStream ->
                val jsonString = jsonObject.toString(4)
                outputStream.write(jsonString.toByteArray())
                outputStream.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(fileUri, contentValues, null, null)

            Log.d("JsonSave", "JSON saved to Documents: $fileUri")
            return fileUri
        } else {
            Log.e("JsonSave", "Failed to save JSON to Documents")
            return null
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveStringToDocuments(context: Context, content: Any, fileName: String = "data.txt"): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val documentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileUri = resolver.insert(documentUri, contentValues)

        if (fileUri != null) {
            resolver.openOutputStream(fileUri)?.use { outputStream ->
                val stringContent = content.toString()  // Convert Any to String
                outputStream.write(stringContent.toByteArray())
                outputStream.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(fileUri, contentValues, null, null)

            Log.d("StringSave", "Content saved to Documents: $fileUri")
            return fileUri
        } else {
            Log.e("StringSave", "Failed to save content to Documents")
            return null
        }
    }

    fun parseHtmlToJson(htmlResponse: String): JSONObject {
        // Extract the <p> content manually (since the structure is known)
        val regex = "<p>(.*?)</p>".toRegex()
        val match = regex.find(htmlResponse)
        val encodedJson = match?.groups?.get(1)?.value ?: ""

        // Convert HTML entities (e.g., &quot;) to normal characters
        val jsonString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(encodedJson, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            Html.fromHtml(encodedJson).toString()
        }

        // Convert to JSONObject
        return JSONObject(jsonString)
    }
}