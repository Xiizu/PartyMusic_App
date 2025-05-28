package com.example.partymusicapp.support

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.R
import com.example.partymusicapp.model.Music

class MusicAdapter(private val items: MutableList<Music>) :
    RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.music_title)
        val proposer: TextView = view.findViewById(R.id.music_proposer)
        val linkButton: ImageButton = view.findViewById(R.id.link_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    private var currentPlayingMusic: Music? = null
//    fun setCurrentPlayingMusic(music: Music?) {
//        currentPlayingMusic = music
//        notifyItemChanged(0)
//    }


    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = items[position]
        holder.title.text = music.title
        holder.proposer.text = music.user_name
        holder.linkButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://youtu.be/${music.link}".toUri())
            it.context.startActivity(intent)
        }

        val isPlaying = music.id == currentPlayingMusic?.id
        holder.itemView.setBackgroundColor(
            if (isPlaying) ContextCompat.getColor(holder.itemView.context, R.color.playingHighlight)
            else Color.TRANSPARENT
        )
    }

    override fun getItemCount(): Int = items.size

    fun addItem(music: Music) {
        if (items.none { it.id == music.id }) {
            items.add(music)
            notifyItemInserted(items.size - 1)
        }
    }

    fun removeItem(music: Music) {
        val index = items.indexOf(music)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun clear() {
        items.clear()
        notifyItemRangeChanged(0,items.size)
    }
//
//    fun mooveToFirst(music: Music) {
//        val index = items.indexOf(music)
//        if (index != -1) {
//            items.removeAt(index)
//            items.add(0, music)
//            notifyItemMoved(index, 0)
//        }
//    }
//
//    fun mooveToLast(music: Music) {
//        val index = items.indexOf(music)
//        if (index != -1) {
//            items.removeAt(index)
//            items.add(items.size, music)
//            notifyItemMoved(index, items.size)
//        }
//    }
//
//    fun getIndexOf(music: Music) : Int {
//        return items.indexOf(music)
//    }
//
//    fun getPlaylistSize() : Int {
//        return items.size
//    }
//
//    fun updateList(newList: List<Music>) {
//        items.clear()
//        items.addAll(newList)
//        notifyItemRangeInserted(0, newList.size)
//    }

}
