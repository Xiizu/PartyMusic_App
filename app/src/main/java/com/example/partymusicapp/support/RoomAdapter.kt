package com.example.partymusicapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
import com.example.partymusicapp.activity.BaseActivity
import com.example.partymusicapp.model.Room

class RoomAdapter(
    private val baseActivity: BaseActivity,
    private var rooms: List<Room>
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.room_label)
        val host: TextView = itemView.findViewById(R.id.room_host)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.label.text = room.label
        holder.host.text = room.host_name

        holder.itemView.setOnClickListener {
            baseActivity.goToRoom(room)
        }
    }

    override fun getItemCount(): Int = rooms.size

    fun updateRooms(newRooms: List<Room>) {
        this.rooms = newRooms
        notifyDataSetChanged()
    }
}
