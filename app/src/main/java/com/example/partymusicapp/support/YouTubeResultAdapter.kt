package com.example.partymusicapp.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.R
import com.example.partymusicapp.model.YouTubeVideoItem
import com.bumptech.glide.Glide


interface OnYouTubeVideoClickListener {
    fun onYouTubeVideoClick(video: YouTubeVideoItem)
}

class YouTubeResultAdapter(
    private val videos: MutableList<YouTubeVideoItem>,
    private val listener: OnYouTubeVideoClickListener
) : RecyclerView.Adapter<YouTubeResultAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById<TextView>(R.id.videoTitle)
        val thumbnail : ImageView = view.findViewById<ImageView>(R.id.videoThumbnail)
        val artist : TextView = view.findViewById<TextView>(R.id.videoArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_youtube_result, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videos[position]
        holder.title.text = item.snippet.title
        holder.artist.text = item.snippet.channelTitle
        Glide.with(holder.itemView)
            .load(item.snippet.thumbnails.default.url)
            .into(holder.thumbnail)

        var musicDAO = MusicDAO()
        musicDAO.init(holder.itemView.context)

        var userDAO = UserDAO()
        userDAO.init(holder.itemView.context)

        holder.itemView.setOnClickListener {
            listener.onYouTubeVideoClick(item)
        }
    }

    fun addItem(video: YouTubeVideoItem) {
        if (videos.none { it.id.videoId == video.id.videoId }) {
            videos.add(video)
            notifyItemInserted(videos.size - 1)
        }
    }

    fun clear() {
        videos.clear()
        notifyItemRangeRemoved(0,videos.size)
    }

    override fun getItemCount() = videos.size
}
