package com.avs.avs_ekyc.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.R

class ImageGridAdapter {
    class ImageGridAdapter(
        private val imageUris: Array<Uri?>,
        private val imageNames: Array<String>,
        private val onImageClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageSlot: ImageView = itemView.findViewById(R.id.imageSlot)
            val imageName : TextView = itemView.findViewById(R.id.imageText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun getItemCount() = 4

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val uri = imageUris[position]
            val name = imageNames[position]

            holder.imageName.text = name
            if (uri != null) {
                holder.imageSlot.setImageURI(uri)
            } else {
                holder.imageSlot.setImageResource(android.R.drawable.ic_menu_camera)
            }

            holder.imageSlot.setOnClickListener {
                onImageClick(position)
            }
        }

        fun updateImage(position: Int, uri: Uri) {
            imageUris[position] = uri
            notifyItemChanged(position)
        }
    }
}
