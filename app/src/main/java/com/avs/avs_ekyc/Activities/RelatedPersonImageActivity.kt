package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.Adapter.ImageGridAdapter.ImageGridAdapter
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityIndivdualPhotoBinding
import com.avs.avs_ekyc.databinding.ActivityRelatedPersonImageBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RelatedPersonImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatedPersonImageBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageGridAdapter

    private val imageUris = arrayOfNulls<Uri>(4)
    private val base64Images = arrayOfNulls<String>(4)
    private val indivdualBase64Images = arrayOfNulls<String>(4)

    private var currentSlotIndex = -1
    private var currentPhotoPath: String? = null
    private lateinit var progressDialog: CustomProgressDialog


    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null && currentSlotIndex != -1) {
                    imageUris[currentSlotIndex] = resultUri
                    adapter.updateImage(currentSlotIndex, resultUri)
                    encodeImageToBase64(resultUri, currentSlotIndex)
                }
            }
        }


    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                startCrop(it)
            }
        }


    private val cameraPicker =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath != null) {
                val fileUri = Uri.fromFile(File(currentPhotoPath!!))
                startCrop(fileUri)
            }
        }


    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(50)
            setFreeStyleCropEnabled(true)
        }

        val uCrop = UCrop.of(sourceUri, destinationUri).withOptions(options).withAspectRatio(1f, 1f)

        cropImageLauncher.launch(uCrop.getIntent(this))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatedPersonImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = CustomProgressDialog(this@RelatedPersonImageActivity)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        val type = intent.getStringExtra("type")


        recyclerView = binding.relatedPersonDetails


        if (type == "3") {
            binding.actionBar.toolbar.title = " Society Photo"
            val imagePlaceHoler = arrayOf(
                R.drawable.registration_of_certificate,
                R.drawable.coi,
                R.drawable.pancard,
                R.drawable.adress_proof
            )
            adapter = ImageGridAdapter(
                this@RelatedPersonImageActivity, imageUris, imagePlaceHoler
            ) { index ->
                currentSlotIndex = index
                showImagePickerDialog()
            }
        } else {
            binding.actionBar.toolbar.title = "Related Person Photo"
            val imagePlaceHolder = arrayOf(
                R.drawable.photo,
                R.drawable.pan,
                R.drawable.add_proof_front,
                R.drawable.add_proof_back
            )
            adapter = ImageGridAdapter(
                this@RelatedPersonImageActivity, imageUris, imagePlaceHolder
            ) { index ->
                currentSlotIndex = index
                showImagePickerDialog()
            }
        }


        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter


        if (type == "3") {
            binding.textRelated.text = "Society / Industry / Trusty Details"
            binding.relatedButton.text = "Next"
            binding.relatedButton.setOnClickListener {

                val type = intent.getStringExtra("type")
                val customerNo = intent.getStringExtra("customerNo")

                if (base64Images.all { it != null }) {
                    // Join array to single string, each Base64 image separated by a delimiter
                    val joinedImages = base64Images.joinToString("||") { it ?: "" }

                    // Save to file
                    val file = File.createTempFile("image_related", ".txt", cacheDir)
                    file.writeText(joinedImages)

                    // Pass only the file path
                    val intent =
                        Intent(this@RelatedPersonImageActivity, SocietyImageActivity::class.java)
                    intent.putExtra("relatedPersonImageFilePath", file.absolutePath)
                    intent.putExtra("type", type)
                    intent.putExtra("customerNo", customerNo)
                    startActivity(intent)
                } else {
                    Constant.error(this@RelatedPersonImageActivity, "Please Upload All Images")
                }
            }
        } else if (type == "2") {
            binding.relatedButton.text = "Upload Images"
            binding.relatedButton.setOnClickListener {
                if (base64Images.all { it != null }) {
                    fetchEncryptedValue()
                } else {
                    Constant.error(this@RelatedPersonImageActivity, "Please Upload All Images")
                }
            }
        }
    }

    fun fetchEncryptedValue() {
        Thread {

            runOnUiThread {
                progressDialog.show()
            }

            val agentNo = SharedPreferenceManager.getString(SharedPreferenceManager.AGENT_NO)
            val customerNo = intent.getStringExtra("customerNo")
            val type = intent.getStringExtra("type")

            val imagePath = intent.getStringExtra("imageFilePath")
            val joinedImages = imagePath?.let { File(it).readText() }
            val individualPhotos = joinedImages?.split("||")?.toTypedArray()
            if (individualPhotos != null) {
                for (i in individualPhotos.indices) {
                    if (individualPhotos[i].isNotEmpty()) {
                        indivdualBase64Images[i] = individualPhotos[i]
                    }
                }
            }

            val modelJson = JSONObject().apply {
                put("custNo", customerNo.toString())
                put("Agentcode", agentNo.toString())
                put("type", type)
                put("legalRemark", "")
                put("ScietyRemarkType", "")
                put("relatedRemark", "")
                put("RelatedRemarkType", "")
                put("file", indivdualBase64Images.getOrNull(0) ?: "")
                put("file1", indivdualBase64Images.getOrNull(1) ?: "")
                put("file2", indivdualBase64Images.getOrNull(2) ?: "")
                put("file3", indivdualBase64Images.getOrNull(3) ?: "")
                put("file4", base64Images[0] ?: "")
                put("file5", base64Images[1] ?: "")
                put("file6", base64Images[2] ?: "")
                put("file7", base64Images[3] ?: "")
            }

            val encryptedData = cleanEncryptedString(AESCryptoUtil.encrypt(modelJson.toString()))

            val urlString = "https://ckyc.tbsbl.com/TBSBCKYC_APP/ImageScanner.asmx/getImageCkyc"

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/plain")
                connection.setRequestProperty("Accept", "text/html")


                // Write the POST data to the output stream
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(encryptedData)
                outputStream.flush()
                outputStream.close()

                // Read the response
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val html = response.toString()

                // Extract JSON string from HTML <p> tag
                val start = html.indexOf("{")
                val end = html.lastIndexOf("}") + 1
                val encodedJson = html.substring(start, end)

                // Decode HTML entities like &quot; => "
                val decodedJson = Html.fromHtml(encodedJson, Html.FROM_HTML_MODE_LEGACY).toString()

                // Parse JSON
                val jsonObject = JSONObject(decodedJson)
                val encryptedValue = jsonObject.getString("encrypted")

                // Use result on UI thread
                runOnUiThread {
                    progressDialog.dismiss()
                    uploadImages(encryptedValue)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HTTP_ERROR", "Error fetching data: ${e.message}")
            }
        }.start()
    }


    private fun uploadImages(encryptedValue: String) {
        val decryptedResponse = AESCryptoUtil.decrypt(encryptedValue ?: "")
        if (!decryptedResponse.isNullOrEmpty() && decryptedResponse.trim().startsWith("{")) {
            val jsonObject = JSONObject(decryptedResponse)
            val status = jsonObject.optString("message")

            if (status.equals("Success", ignoreCase = true)) {
                Constant.success(
                    this@RelatedPersonImageActivity, "Image Uploaded Successfully"
                )
                startActivity(
                    Intent(
                        this@RelatedPersonImageActivity, ShowPendingListActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                Constant.error(this@RelatedPersonImageActivity, status)
            }
        } else {
            Constant.error(this@RelatedPersonImageActivity, "Invalid response")
        }
    }

    private fun cleanEncryptedString(dirtyString: String): String {
        return dirtyString.replace("\n", "").replace("\\u003d", "=").replace("\"", "")
    }

//    private fun String.toPlainRequestBody(): RequestBody {
//        return RequestBody.create("text/plain".toMediaTypeOrNull(), this)
//    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this).setTitle("Select Image").setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }.show()
    }

    private fun openGallery() {
        imagePicker.launch("image/*")
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        val photoUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", // Make sure provider is properly set in Manifest
            photoFile
        )
        currentPhotoPath = photoFile.absolutePath
        cameraPicker.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", storageDir
        )
    }

    private fun encodeImageToBase64(uri: Uri, index: Int) {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
        base64Images[index] = base64String
        Log.d("Base64[$index]", base64String.take(40) + "...")
    }
}