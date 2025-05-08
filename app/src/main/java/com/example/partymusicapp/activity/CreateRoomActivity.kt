package com.example.partymusicapp.activity

import android.R.attr.data
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
import com.example.partymusicapp.activity.LoginActivity
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.RoomDAO
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateRoomActivity : BaseActivity() {

    lateinit var label : EditText
    lateinit var description : EditText
    lateinit var createButton : FloatingActionButton
    lateinit var progressBars : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTracker.register(this)
        setContentView(R.layout.activity_create_room)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        description = findViewById<TextInputEditText>(R.id.description)
        label = findViewById<TextInputEditText>(R.id.label)
        createButton = findViewById<FloatingActionButton>(R.id.create_room_button)
        progressBars = findViewById(R.id.progress_spinner)

        progressBars.visibility = ProgressBar.INVISIBLE
        description.movementMethod = ScrollingMovementMethod.getInstance()

        fun checkInputs(){
            val labelText = label.text.toString()
            createButton.isEnabled = labelText.isNotEmpty()
        }

        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        description.addTextChangedListener(inputWatcher)
        label.addTextChangedListener(inputWatcher)

        createButton.setOnClickListener {
            val labelText = label.text.toString()
            val descriptionText = description.text.toString()
            val user_id = user.id

            createButton.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            val roomDAO = RoomDAO()
            roomDAO.init(this)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.createRoom(
                        ApiService.CreateRoomRequest(
                            id = user_id,
                            label = labelText,
                            description = descriptionText
                        )
                    )
                    val body = response.body()

                    if (body != null && body.statut == "success" && body.data != null) {
                        val newRoom = body.data
                        roomDAO.init(this@CreateRoomActivity)
                        roomDAO.insert(newRoom)

                        startActivity(Intent(this@CreateRoomActivity, MainActivity::class.java).apply {
                            putExtra("ROOM_ID", newRoom.id)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })

                        BaseActivity.shouldRecreate = true
                        finish()
                        Log.i("MainActivity", "CreateRoom Request Success - " + response.toString())
                    } else if (response.code() == 433) {
                        Toast.makeText(this@CreateRoomActivity,getString(R.string.info_max_room_reached),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "CreateRoom Request Error - " + response.toString())
                    } else if (response.code() == 400) {
                        Toast.makeText(this@CreateRoomActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "CreateRoom Request Error - " + response.toString())
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@CreateRoomActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Login Request Error - " + e.toString())
                } finally {
                    createButton.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                    roomDAO.close()
                }
            }

        }




    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityTracker.unregister(this)
    }

}
