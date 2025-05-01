package com.avs.avs_ekyc.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avs.avs_ekyc.R
import com.bumptech.glide.Glide

class ImageGridAdapter {
    class ImageGridAdapter(
        private val context : Context,
        private val imageUris: Array<Uri?>,
        private val imagePlaceHolder : Array<Int>,
        private val onImageClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageSlot: ImageView = itemView.findViewById(R.id.imageSlot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun getItemCount() = 4

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val uri = imageUris[position]
            val placeHolder = imagePlaceHolder[position]

            if (uri != null) {
                Glide.with(context)
                    .load(uri)
                    .placeholder(placeHolder)
                    .error(android.R.drawable.ic_menu_camera)
                    .into(holder.imageSlot)
            } else {
                holder.imageSlot.setImageResource(placeHolder)
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
