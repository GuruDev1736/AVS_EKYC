package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import java.io.File
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


    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null && currentSlotIndex != -1) {
                imageUris[currentSlotIndex] = resultUri
                adapter.updateImage(currentSlotIndex, resultUri)
                encodeImageToBase64(resultUri, currentSlotIndex)
            }
        }
    }


    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            startCrop(it)
        }
    }


    private val cameraPicker = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
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

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)

        cropImageLauncher.launch(uCrop.getIntent(this))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatedPersonImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        val type = intent.getStringExtra("type")


        recyclerView = binding.relatedPersonDetails


        if (type =="3")
        {
            binding.actionBar.toolbar.title = " Society Photo"
            val imagePlaceHoler = arrayOf(R.drawable.registration_of_certificate, R.drawable.coi, R.drawable.pancard, R.drawable.adress_proof)
            adapter = ImageGridAdapter(this@RelatedPersonImageActivity,imageUris,imagePlaceHoler) { index ->
                currentSlotIndex = index
                showImagePickerDialog()
            }
        }
        else
        {
            binding.actionBar.toolbar.title = "Related Person Photo"
            val imagePlaceHolder = arrayOf(R.drawable.photo, R.drawable.pan, R.drawable.add_proof_front, R.drawable.add_proof_back)
            adapter = ImageGridAdapter(this@RelatedPersonImageActivity,imageUris,imagePlaceHolder) { index ->
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
                    val intent = Intent(this@RelatedPersonImageActivity, SocietyImageActivity::class.java)
                    intent.putExtra("relatedPersonImageFilePath", file.absolutePath)
                    intent.putExtra("type", type)
                    intent.putExtra("customerNo", customerNo)
                    startActivity(intent)
                } else {
                    Constant.error(this@RelatedPersonImageActivity, "Please Upload All Images")
                }
            }
        } else if(type == "2"){
            binding.relatedButton.text = "Upload Images"
            binding.relatedButton.setOnClickListener {
                if (base64Images.all { it != null }) {
                    uploadImages()
                }
                else {
                    Constant.error(this@RelatedPersonImageActivity, "Please Upload All Images")
                }
            }
        }
    }

    private fun uploadImages() {
        val progress = CustomProgressDialog(this)
        progress.show()

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

        val encryptedData =
            cleanEncryptedString(AESCryptoUtil.encrypt(modelJson.toString())).toPlainRequestBody()

        RetrofitInstance.getInstance().getImageCkyc(encryptedData)
            .enqueue(
                object : Callback<UniversalResponseModel> {
                    override fun onResponse(
                        call: Call<UniversalResponseModel>,
                        response: Response<UniversalResponseModel>
                    ) {
                        progress.dismiss()
                        if (response.isSuccessful) {
                            val encryptedResponse = response.body()?.encrypted
                            val decryptedResponse = AESCryptoUtil.decrypt(encryptedResponse ?: "")
                            if (!decryptedResponse.isNullOrEmpty() && decryptedResponse.trim()
                                    .startsWith("{")
                            ) {
                                val jsonObject = JSONObject(decryptedResponse)
                                val status = jsonObject.optString("message")

                                if (status.equals("Success", ignoreCase = true)) {
                                    Constant.success(
                                        this@RelatedPersonImageActivity,
                                        "Image Uploaded Successfully"
                                    )
                                    startActivity(
                                        Intent(
                                            this@RelatedPersonImageActivity,
                                            ShowPendingListActivity::class.java
                                        )
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    Constant.error(this@RelatedPersonImageActivity, status)
                                }
                            } else {
                                Constant.error(this@RelatedPersonImageActivity, "Invalid response")
                            }
                        } else {
                            Constant.error(this@RelatedPersonImageActivity, "Server error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        Constant.error(
                            this@RelatedPersonImageActivity,
                            "Failed: ${t.localizedMessage}"
                        )
                    }
                })
    }

    private fun cleanEncryptedString(dirtyString: String): String {
        return dirtyString.replace("\n", "").replace("\\u003d", "=").replace("\"", "")
    }

    private fun String.toPlainRequestBody(): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), this)
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        imagePicker.launch("image/*")
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider", // Make sure provider is properly set in Manifest
            photoFile
        )
        currentPhotoPath = photoFile.absolutePath
        cameraPicker.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
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