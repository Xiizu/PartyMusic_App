package com.example.partymusicapp.support

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = items[position]
        holder.title.text = music.title
        holder.proposer.text = music.user_name
        holder.linkButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(music.link))
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(music: Music) {
        if (items.none { it.id == music.id }) {
            items.add(music)
            notifyItemInserted(items.size - 1)
        }
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}
