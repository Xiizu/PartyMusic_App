package com.example.partymusicapp.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.partymusicapp.R
import com.example.partymusicapp.support.ActivityTracker


class UserSettingsActivity : BaseActivity() {

    lateinit var logoutButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {

        // Contenu settings
        super.onCreate(savedInstanceState)
        // suivre les activités ouvertes
        ActivityTracker.register(this)
        // afficher la vue
        setContentView(R.layout.activity_user_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logoutButton = findViewById<Button>(R.id.button_logout)
        logoutButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.info_logout), Toast.LENGTH_SHORT).show()
            userDAO.open()
            userDAO.empty()
            userDAO.close()
            recreate()
        }
    }
}
