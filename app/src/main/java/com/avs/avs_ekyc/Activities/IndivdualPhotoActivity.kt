package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.Adapter.ImageGridAdapter.ImageGridAdapter
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityIndivdualPhotoBinding
import java.io.File

class IndivdualPhotoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityIndivdualPhotoBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageGridAdapter

    private val imageUris = arrayOfNulls<Uri>(4)
    private val base64Images = arrayOfNulls<String>(4)

    private var currentSlotIndex = -1


    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUris[currentSlotIndex] = it
            adapter.updateImage(currentSlotIndex, it)
            encodeImageToBase64(it, currentSlotIndex)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIndivdualPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Individual Photo"

        recyclerView = binding.rvImageGrid

        adapter = ImageGridAdapter(imageUris) { index ->
            currentSlotIndex = index
            imagePicker.launch("image/*")
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        binding.next.setOnClickListener {

            val type = intent.getStringExtra("type")
            val customerNo = intent.getStringExtra("customerNo")

            if (base64Images.all { it != null }) {
                // Join array to single string, each Base64 image separated by a delimiter
                val joinedImages = base64Images.joinToString("||") { it ?: "" }

                // Save to file
                val file = File.createTempFile("image_", ".txt", cacheDir)
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

    private fun encodeImageToBase64(uri: Uri, index: Int) {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
        base64Images[index] = base64String
        Log.d("Base64[$index]", base64String.take(40) + "...")
    }
}