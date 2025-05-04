package com.example.partymusicapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.activity.BaseActivity
import com.example.partymusicapp.model.Music
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.MusicAdapter

class MainActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MusicAdapter
    private lateinit var roomName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTracker.register(this)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars: Insets = insets.getInsets(Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.next_music_list)
        adapter = MusicAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        roomName = findViewById(R.id.room_name)

        update()
    }

    private fun displayRoomView(room: Room) {
        roomName.text = room.label
        musicDAO.init(this)
        musicDAO.open()
        val musics = musicDAO.index(room.id)
        adapter.clear()
        musics.forEach { music ->
            adapter.addItem(music)
        }
        musicDAO.close()
    }

    private fun displayNullView() {
        roomName.text = user.name
    }

    private fun update() {
        val roomId = intent.getIntExtra("ROOM_ID", -1)
        val fetchedRoom = if (roomId != -1) roomDAO.get(roomId) else null
        if (fetchedRoom == null) {
            displayNullView()
        } else {
            displayRoomView(fetchedRoom)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityTracker.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        update()
    }
}
