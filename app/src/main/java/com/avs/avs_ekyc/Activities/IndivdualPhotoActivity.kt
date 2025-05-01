package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.Activities.RelatedPersonImageActivity
import com.avs.avs_ekyc.Adapter.ImageGridAdapter.ImageGridAdapter
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityIndivdualPhotoBinding
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

class IndivdualPhotoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityIndivdualPhotoBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageGridAdapter

    private val imageUris = arrayOfNulls<Uri>(4)
    private val base64Images = arrayOfNulls<String>(4)

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



    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIndivdualPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Individual Photo"

        recyclerView = binding.rvImageGrid

        val imagePlaceHolders = arrayOf(R.drawable.photo, R.drawable.pan, R.drawable.add_proof_front, R.drawable.add_proof_back)
        adapter = ImageGridAdapter(this@IndivdualPhotoActivity,imageUris,imagePlaceHolders) { index ->
            currentSlotIndex = index
            showImagePickerDialog()
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        val type = intent.getStringExtra("type")

        if (type =="1")
        {
            binding.next.text = "Upload Images"

            binding.next.setOnClickListener {
                if (base64Images.all { it != null }) {
                    uploadImages()
                } else {
                    Constant.error(this, "Please select all images")
                }
            }
        }

        if (type == "2")
        {
            binding.next.text = "Next"

            binding.next.setOnClickListener {

                val type = intent.getStringExtra("type")
                val customerNo = intent.getStringExtra("customerNo")

                if (base64Images.all { it != null }) {
                    // Join array to single string, each Base64 image separated by a delimiter
                    val joinedImages = base64Images.joinToString("||") { it ?: "" }

                    // Save to file
                    val file = File.createTempFile("image_individual", ".txt", cacheDir)
                    file.writeText(joinedImages)

                    // Pass only the file path
                    val intent = Intent(this@IndivdualPhotoActivity, RelatedPersonImageActivity::class.java)
                    intent.putExtra("imageFilePath", file.absolutePath)
                    intent.putExtra("type", type)
                    intent.putExtra("customerNo", customerNo)
                    startActivity(intent)
                }
                else{
                    Constant.error(this@IndivdualPhotoActivity, "Please Upload All Images")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun uploadImages() {
        val progress = CustomProgressDialog(this)
        progress.show()

        val agentNo = SharedPreferenceManager.getString(SharedPreferenceManager.AGENT_NO)
        val customerNo = intent.getStringExtra("customerNo")
        val type = intent.getStringExtra("type")

        val modelJson = JSONObject().apply {
            put("custNo", customerNo.toString())
            put("Agentcode", agentNo.toString())
            put("type", type)
            put("legalRemark", "")
            put("ScietyRemarkType", "")
            put("relatedRemark", "")
            put("RelatedRemarkType", "")
            put("file", base64Images[0] ?: "")
            put("file1", base64Images[1] ?: "")
            put("file2", base64Images[2] ?: "")
            put("file3", base64Images[3] ?: "")
            put("file4", "")
            put("file5", "")
            put("file6", "")
            put("file7", "")
        }

        //Constant.saveJsonToDownloads(this@IndivdualPhotoActivity,modelJson)

        val encryptedData =
            cleanEncryptedString(AESCryptoUtil.encrypt(modelJson.toString())).toPlainRequestBody()

       // Constant.saveStringToDocuments(this@IndivdualPhotoActivity,cleanEncryptedString(AESCryptoUtil.encrypt(modelJson.toString())))

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
                                        this@IndivdualPhotoActivity,
                                        "Image Uploaded Successfully"
                                    )
                                    startActivity(
                                        Intent(
                                            this@IndivdualPhotoActivity,
                                            ShowPendingListActivity::class.java
                                        )
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    Constant.error(this@IndivdualPhotoActivity, status)
                                }
                            } else {
                                Constant.error(this@IndivdualPhotoActivity, "Invalid response")
                            }
                        } else {
                            Constant.error(this@IndivdualPhotoActivity, "Server error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        Constant.error(
                            this@IndivdualPhotoActivity,
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