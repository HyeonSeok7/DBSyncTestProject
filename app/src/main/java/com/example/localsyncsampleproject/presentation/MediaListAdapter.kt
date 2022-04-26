package com.example.localsyncsampleproject.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.datastore.preferences.core.preferencesOf
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localsyncsampleproject.data.room.entity.Media
import com.example.localsyncsampleproject.databinding.ItemPhotoBinding

class MediaListAdapter : RecyclerView.Adapter<MediaListAdapter.MediaListViewHolder>() {

    private var items: MutableList<Media> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPhotoBinding.inflate(layoutInflater, parent, false)
        return MediaListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaListViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: Media) {
        items.add(item)
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    class MediaListViewHolder(private var binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: Media) {
            Glide.with(itemView)
                .load(media.path)
                .into(binding.ivPhoto)
        }
    }

}