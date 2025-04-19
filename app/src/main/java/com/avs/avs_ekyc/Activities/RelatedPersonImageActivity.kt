package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.Activities.GetCustomerInformationActivity
import com.avs.avs_ekyc.Activities.ShowPendingListActivity
import com.avs.avs_ekyc.Adapter.ImageGridAdapter.ImageGridAdapter
import com.avs.avs_ekyc.Adapter.PendingListAdapter
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.PendingCustomer
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.Model.UploadImageModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityRelatedPersonImageBinding
import com.google.gson.Gson
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

class RelatedPersonImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatedPersonImageBinding
    private lateinit var adapter: ImageGridAdapter

    private val societyImageUris = arrayOfNulls<Uri>(4)
    private val relatedImageUris = arrayOfNulls<Uri>(4)
    private val societyBase64Images = arrayOfNulls<String>(4)
    private val relatedBase64Images = arrayOfNulls<String>(4)
    private val indivdualBase64Images = arrayOfNulls<String>(4)

    private var currentSlotIndex = -1

    private val societyImagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                societyImageUris[currentSlotIndex] = it
                adapter.updateImage(currentSlotIndex, it)
                encodeImageToBase64(it, currentSlotIndex, societyBase64Images)
            }
        }

    private val relatedImagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                relatedImageUris[currentSlotIndex] = it
                adapter.updateImage(currentSlotIndex, it)
                encodeImageToBase64(it, currentSlotIndex, relatedBase64Images)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatedPersonImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Related Person Image"

        val type = intent.getStringExtra("type").toString()

        if (type == "1" || type == "2") {
            val imagePath = intent.getStringExtra("imageFilePath")
            val joinedImages = imagePath?.let { File(it).readText() }
            val individualPhoto = joinedImages?.split("||")?.toTypedArray()
            if (individualPhoto != null) {
                for (i in individualPhoto.indices) {
                    if (individualPhoto[i].isNotEmpty()) {
                        indivdualBase64Images[i] = individualPhoto[i]
                    }
                }
            }
            val customerNo = intent.getStringExtra("customerNo")
            binding.societyLayout.visibility = View.GONE
            binding.relatedPersonDetails.layoutManager = GridLayoutManager(this, 2)
            adapter = ImageGridAdapter(relatedImageUris) { index ->
                currentSlotIndex = index
                relatedImagePicker.launch("image/*")
            }
            binding.relatedPersonDetails.adapter = adapter

            binding.uploadImages.setOnClickListener {
                if (relatedBase64Images.all { it != null }) {
                    callUploadImage(relatedBase64Images,
                        indivdualBase64Images, customerNo, type)
                }
            }
        } else {
            val customerNo = intent.getStringExtra("customerNo")
            binding.societyLayout.visibility = View.VISIBLE
            binding.relatedPersonDetails.layoutManager = GridLayoutManager(this, 2)
            adapter = ImageGridAdapter(relatedImageUris) { index ->
                currentSlotIndex = index
                relatedImagePicker.launch("image/*")
            }
            binding.relatedPersonDetails.adapter = adapter

            binding.societyImages.layoutManager = GridLayoutManager(this, 2)
            adapter = ImageGridAdapter(relatedImageUris) { index ->
                currentSlotIndex = index
                societyImagePicker.launch("image/*")
            }
            binding.societyImages.adapter = adapter


            binding.uploadImages.setOnClickListener {
                if (relatedBase64Images.all { it != null } && societyBase64Images.all { it != null }) {
                    callUploadImage(relatedBase64Images, societyBase64Images, customerNo, type)
                }
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri, index: Int, base64Images: Array<String?>) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Compress to 50% quality

            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            base64Images[index] = base64String

            Log.d("Base64[$index]", base64String.take(40) + "...")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageEncodeError", "Failed to encode image at index $index: ${e.message}")
        }
    }


    private fun callUploadImage(
        relatedBase64Images: Array<String?>,
        societyBase64Images: Array<String?>,
        customerNo: String?,
        type: String
    ) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val agentNo = SharedPreferenceManager.getString(SharedPreferenceManager.AGENT_NO)

        val model = UploadImageModel(
            customerNo.toString(), agentNo, type,
            relatedBase64Images[0].toString(),
            relatedBase64Images[1].toString(),
            relatedBase64Images[2].toString(),
            relatedBase64Images[3].toString(),
            societyBase64Images[0].toString(),
            societyBase64Images[1].toString(),
            societyBase64Images[2].toString(),
            societyBase64Images[3].toString()
        )
        val json = Gson().toJson(model)
        val encryptedData = AESCryptoUtil.encrypt(json)

        try {
            RetrofitInstance.getInstance().getImageCkyc(encryptedData)
                .enqueue(object : Callback<UniversalResponseModel> {
                    override fun onResponse(
                        call: Call<UniversalResponseModel>,
                        response: Response<UniversalResponseModel>
                    ) {
                        progress.dismiss()

                        if (response.isSuccessful) {
                            val encryptedResponse = response.body()?.encrypted
                            val decryptedResponse = AESCryptoUtil.decrypt((encryptedResponse ?: ""))

                            Log.d("EncryptedRes", (encryptedResponse ?: "null"))
                            Log.d("DecryptedRes", decryptedResponse ?: "null")

                            if (!decryptedResponse.isNullOrEmpty()) {
                                try {
                                    if (decryptedResponse.trim().startsWith("[")) {
                                        try {
                                            if (decryptedResponse.trim().startsWith("{")) {
                                                val jsonObject = JSONObject(decryptedResponse)
                                                val status = jsonObject.getString("message")
                                                if (status.equals("Success", ignoreCase = true)) {
                                                    Constant.success(this@RelatedPersonImageActivity,"Image Uploaded Successfully")
                                                } else {
                                                    Constant.error(
                                                        this@RelatedPersonImageActivity,
                                                        "Success failed"
                                                    )
                                                }
                                            } else {
                                                Constant.error(
                                                    this@RelatedPersonImageActivity,
                                                    decryptedResponse
                                                )
                                                Log.e(
                                                    "LoginError",
                                                    "Decrypted response is not JSON"
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e("JSON Error", e.message ?: "Parsing error")
                                            Constant.error(
                                                this@RelatedPersonImageActivity,
                                                "Parsing error"
                                            )
                                        }
                                    } else {
                                        Constant.error(
                                            this@RelatedPersonImageActivity,
                                            "Invalid response format"
                                        )
                                        Log.e("Error", "Decrypted response is not a JSONArray")
                                    }
                                } catch (e: Exception) {
                                    Constant.error(
                                        this@RelatedPersonImageActivity,
                                        "Something went wrong"
                                    )
                                    Log.e("Exception", e.message.toString())
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        if (t is IOException) {
                            Constant.error(
                                this@RelatedPersonImageActivity,
                                "Network issue. Check connection."
                            )
                        } else {
                            Constant.error(
                                this@RelatedPersonImageActivity,
                                "API error: ${t.localizedMessage}"
                            )
                        }
                        Log.e("Error", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("Exception", e.message ?: "Exception")
            Constant.error(this@RelatedPersonImageActivity, "Unexpected error occurred")
        }
    }
}