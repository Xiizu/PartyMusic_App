package com.example.partymusicapp.activity

import android.content.Intent
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
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
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

open class BaseActivity : AppCompatActivity() {

    val userDAO = UserDAO()
    lateinit var user: User
    val roomDAO = RoomDAO()
    lateinit var rooms: ArrayList<Room>
    var currentRoom: Room? = null
    val musicDAO = MusicDAO()

    lateinit var navView: NavigationView
    lateinit var drawerLayout: DrawerLayout
    lateinit var toolbar: Toolbar
    lateinit var titleText: TextView
    lateinit var settingButton: View
    lateinit var footerView: LinearLayout
    lateinit var refreshButton: ImageButton
    lateinit var searchBar: TextInputEditText

    override fun setContentView(layoutResID: Int) {
        val drawer = layoutInflater.inflate(R.layout.activity_base, null)
        val container = drawer.findViewById<FrameLayout>(R.id.base_content)
        layoutInflater.inflate(layoutResID, container, true)
        super.setContentView(drawer)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        navView = findViewById(R.id.nav_view)
        rooms = ArrayList()

        if (setupDrawer()) {
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

    private fun fetchRooms() {
        roomDAO.init(this)
        val initRoomsToAdd = roomDAO.index()
        navView.menu.clear()
        navView.inflateMenu(R.menu.drawer_menu)
        for (room in initRoomsToAdd) {
            val item = navView.menu.add(Menu.NONE, room.id, Menu.NONE, room.label)
            rooms.add(room)
        }
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
                    Log.i("MainActivity", "GetUserRoom Request Success - $response")
                } else if (response.code() == 400 || response.code() == 404) {
                    Log.i("MainActivity", "GetUserRoom Request Error - ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "GetUserRoom Request Error - $e")
            } finally {
                roomDAO.open()
                val roomsToAdd = roomDAO.index()
                navView.menu.clear()
                navView.inflateMenu(R.menu.drawer_menu)
                for (room in roomsToAdd) {
                    val item = navView.menu.add(Menu.NONE, room.id, Menu.NONE, room.label)
                    rooms.add(room)
                }
                roomDAO.close()
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

        val headerView = navView.getHeaderView(0)
        titleText = headerView.findViewById(R.id.header_title)
        searchBar = headerView.findViewById(R.id.search_bar)

        titleText.text = user.name
        settingButton = headerView.findViewById(R.id.button_setting)
        settingButton.setOnClickListener {
            val intent = Intent(this, UserSettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        fetchRooms()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.create -> {
                    if (this::class != MainActivity::class) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    }
                }
                R.id.test -> {
                    Toast.makeText(this, "Test clicked", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val roomId = menuItem.itemId
                    roomDAO.open()
                    val selectedRoom = roomDAO.get(roomId)
                    roomDAO.close()
                    if (selectedRoom == null) {
                        Toast.makeText(this, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        intent.putExtra("ROOM_ID", selectedRoom.id)
                        startActivity(intent)
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        footerView = navView.findViewById(R.id.footer)
        footerView.setOnClickListener {
            if (this::class != CreateRoomActivity::class) {
                val intent = Intent(this, CreateRoomActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        refreshButton = navView.findViewById(R.id.button_setting)
        refreshButton.setOnClickListener {
            fetchRooms()
        }
        return true
    }

    private fun joinRoom(code: String) {
        searchBar.clearFocus()
        searchBar.isEnabled = false
        Log.e("MainActivity", user.id.toString())

        lifecycleScope.launch {
            // Logique de jointure de room ici
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
