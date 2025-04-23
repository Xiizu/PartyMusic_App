package com.example.partymusicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.partymusicapp.activity.BaseActivity
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.support.ActivityTracker


class MainActivity : BaseActivity() {

    lateinit var username : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTracker.register(this)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        username = findViewById<TextView>(R.id.username)
        update()
    }

    private fun displayRoomView(room: Room){
        username.text = room.label
    }

    private fun displayNullView(){
        username.text = user.name
    }

    private fun update() {
        var roomId = intent.getIntExtra("ROOM_ID", -1)
        var fetchedRoom = if (roomId != -1) roomDAO.get(roomId) else null
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
