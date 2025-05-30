package com.example.partymusicapp.activity

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.partymusicapp.R
import com.example.partymusicapp.support.ActivityTracker


class UserSettingsActivity : BaseActivity() {

    // initialisation des elements de la vue
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

        // Assignation des éléments de la vue
        logoutButton = findViewById<Button>(R.id.button_logout)

        // Détecter le click sur le bouton de déconnexion
        logoutButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.info_logout), Toast.LENGTH_SHORT).show()
            // Supprimer toutes les données de l'utilisateur
            userDAO.init(this)
            userDAO.open()
            userDAO.empty()
            userDAO.close()
            musicDAO.init(this)
            musicDAO.open()
            musicDAO.empty()
            musicDAO.close()
            roomDAO.init(this)
            roomDAO.open()
            roomDAO.empty()
            roomDAO.close()
            recreate()
        }
    }
}
