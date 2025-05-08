package com.example.partymusicapp.activity

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
import com.example.partymusicapp.adapter.RoomAdapter
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.model.User
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.MusicDAO
import com.example.partymusicapp.support.RoomDAO
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    val userDAO = UserDAO()
    lateinit var user: User
    val roomDAO = RoomDAO()
    lateinit var rooms: ArrayList<Room>
    var currentRoom: Room? = null
    val musicDAO = MusicDAO()

    lateinit var drawerLayout: DrawerLayout
    lateinit var toolbar: Toolbar
    lateinit var titleText: TextView
    lateinit var settingButton: View
    lateinit var footerView: LinearLayout
    lateinit var refreshButton: ImageButton
    lateinit var searchBar: TextInputEditText
    lateinit var roomRecycler: RecyclerView
    lateinit var roomAdapter: RoomAdapter

    override fun setContentView(layoutResID: Int) {
        val drawer = layoutInflater.inflate(R.layout.activity_base, null)
        val container = drawer.findViewById<FrameLayout>(R.id.base_content)
        layoutInflater.inflate(layoutResID, container, true)
        super.setContentView(drawer)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        rooms = ArrayList()

        if (setupDrawer()) {
            var isEditing = false

            searchBar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (!isEditing) {
                        isEditing = true
                        val upperText = s.toString().uppercase(Locale.getDefault())
                        searchBar.setText(upperText)
                        searchBar.setSelection(upperText.length)
                        isEditing = false
                    }
                }
            })
            searchBar.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    val query = searchBar.text.toString()
                    if (query.length == 8) {
                        joinRoom(query)
                    }
                    true
                } else false
            }
        }
    }

    fun goToRoom(room: Room) {
        lifecycleScope.launch {
            fetchMusics(room.id)
            startActivity(Intent(this@BaseActivity, MainActivity::class.java).apply {
                putExtra("ROOM_ID", room.id)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }

    private suspend fun fetchMusics(roomId: Int) {
        try {
            val response = RetrofitClient.instance.getMusic(ApiService.GetMusicRequest(roomId))
            val body = response.body()
            if (body != null && body.statut == "success" && body.data != null) {
                musicDAO.init(this@BaseActivity)
                musicDAO.open()
                musicDAO.emptyRoom(roomId)
                for (music in body.data) {
                    musicDAO.insert(music)
                }
                musicDAO.close()
                Log.i(
                    "MainActivity",
                    "GetMusics Request Success - ${body.data.size} music fetched for room $roomId"
                )
            } else {
                val message = body?.message ?: "Erreur inconnue"
                Log.e("MainActivity", "GetMusics Request Error - $message")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "GetMusics Request Error - $e")
        }
    }

    private fun putAllRooms() {
        roomDAO.open()
        val roomsToAdd = roomDAO.index()
        roomDAO.close()
        rooms.clear()
        rooms.addAll(roomsToAdd)
        roomAdapter.updateRooms(rooms)
    }

    private fun fetchRooms() {
        roomDAO.init(this)
        putAllRooms()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserRoom(ApiService.GetUserRoomRequest(user.id))
                val body = response.body()
                if (body != null && body.statut == "success" && body.data != null) {
                    roomDAO.open()
                    roomDAO.empty()
                    for (room in body.data) {
                        roomDAO.insert(room)
                    }
                    roomDAO.close()
                    Log.i("MainActivity", "GetUserRoom Request Success - ${body.data.size} room fetched")
                } else if (response.code() == 400 || response.code() == 404) {
                    Log.e("MainActivity", "GetUserRoom Request Error - ${response.body()?.message}")
                } else {
                    Log.e("MainActivity", "GetUserRoom Request Error - $response")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "GetUserRoom Request Error - $e")
            } finally {
                putAllRooms()
            }
        }
    }

    private fun setupDrawer(): Boolean {
        userDAO.init(this)
        val connectedUser = userDAO.get()
        if (connectedUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            user = connectedUser
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.info_drawer_oppened,
            R.string.info_drawer_closed
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        titleText = findViewById(R.id.header_title)
        searchBar = findViewById(R.id.search_bar)

        titleText.text = user.name
        settingButton = findViewById(R.id.button_setting)
        settingButton.setOnClickListener {
            val intent = Intent(this, UserSettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        roomRecycler = findViewById(R.id.room_holder)
        roomRecycler.layoutManager = LinearLayoutManager(this)
        roomAdapter = RoomAdapter(this@BaseActivity, rooms)
        roomRecycler.adapter = roomAdapter


        fetchRooms()
        footerView = findViewById(R.id.footer)
        footerView.setOnClickListener {
            if (this::class != CreateRoomActivity::class) {
                val intent = Intent(this, CreateRoomActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        refreshButton = findViewById(R.id.refresh_button)
        refreshButton.setOnClickListener {
            fetchRooms()
        }

        return true
    }

    private fun joinRoom(code: String) {
        searchBar.clearFocus()
        searchBar.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.joinRoom(ApiService.JoinRoomRequest(user.id, code))
                val body = response.body()

                if (body != null && body.statut == "success" && body.data != null) {
                    val joinedRoom = body.data
                    searchBar.text = null
                    // database
                    roomDAO.open()
                    roomDAO.insert(joinedRoom)
                    roomDAO.close()
                    // drawer
                    rooms.add(joinedRoom)
                    roomAdapter.updateRooms(rooms)
                    // vue
                    lifecycleScope.launch {
                        fetchMusics(joinedRoom.id)
                        goToRoom(joinedRoom)
                    }
                    Log.i("MainActivity", "JoinRoom Request Success - $response")
                } else if (response.code() == 400 || response.code() == 404) {
                    Toast.makeText(this@BaseActivity, getString(R.string.error_room_not_found), Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "JoinRoom Request Error - ${body?.message}")
                } else {
                    Toast.makeText(this@BaseActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "JoinRoom Request Unknown Error - $response")
                }
            } catch (e: Exception) {
                Toast.makeText(this@BaseActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "JoinRoom Request Exception - $e")
            } finally {
                searchBar.isEnabled = true
                fetchRooms()
            }
        }
    }

    override fun onBackPressed() {
        if (ActivityTracker.isLastActivity()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_exit))
                .setMessage(getString(R.string.confirm_exit))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> finishAffinity() }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        drawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            fetchRooms()
        }
        if (shouldRecreate) {
            shouldRecreate = false
            recreate()
        }
    }

    companion object {
        var shouldRecreate = false
    }
}
