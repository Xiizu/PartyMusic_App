package com.example.partymusicapp.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.R
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class UserSettingsActivity : BaseActivity() {

    // Initialisation des composants
    lateinit var logoutButton : Button
    lateinit var editUsernameInput : TextInputEditText
    lateinit var editPasswordInput : TextInputEditText
    lateinit var confirmPasswordInput : TextInputEditText
    lateinit var oldPasswordInput : TextInputEditText
    lateinit var saveUsernameButton : Button
    lateinit var savePasswordButton : Button
    lateinit var progressBar : ProgressBar

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
        // Définir les composants
        logoutButton = findViewById<Button>(R.id.button_logout)
        editUsernameInput = findViewById<TextInputEditText>(R.id.text_new_username)
        editPasswordInput = findViewById<TextInputEditText>(R.id.text_new_password)
        confirmPasswordInput = findViewById<TextInputEditText>(R.id.confirm_new_password)
        oldPasswordInput = findViewById<TextInputEditText>(R.id.text_old_password)
        saveUsernameButton = findViewById<Button>(R.id.button_change_username)
        savePasswordButton = findViewById<Button>(R.id.button_change_password)
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        // Définition du clic sur le bouton de déconnexion
        logoutButton.setOnClickListener {
            // Déconnexion et suppression des données
            Toast.makeText(this, getString(R.string.info_logout), Toast.LENGTH_SHORT).show()
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
        // Définition du clic sur le bouton de modification du nom d'utilisateur
        saveUsernameButton.setOnClickListener {
            // Récupérer le contenu des champs
            val newUsername = editUsernameInput.text.toString()
            if(!newUsername.isEmpty() or (newUsername.length >= 3)){
                // Appel de l'api pour modifier le nom d'utilisateur
                editUsername(newUsername)
            } else {
                Toast.makeText(this, getString(R.string.info_short_name), Toast.LENGTH_SHORT).show()
            }
        }
        // Définition du clic sur le bouton de modification du mot de passe
        savePasswordButton.setOnClickListener {
            // Récupérer le contenu des champs
            val oldPassword = oldPasswordInput.text.toString()
            val newPassword = editPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            if(newPassword != confirmPassword){
                Toast.makeText(this, getString(R.string.info_confirm_different), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(newPassword.length < 8){
                Toast.makeText(this, getString(R.string.info_password_need_8chars), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!newPassword.matches(".*[A-Z].*".toRegex())){
                Toast.makeText(this, getString(R.string.info_password_need_upper), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!newPassword.matches(".*[a-z].*".toRegex())){
                Toast.makeText(this, getString(R.string.info_password_need_lower), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!newPassword.matches(".*[0-9].*".toRegex())){
                Toast.makeText(this, getString(R.string.info_password_need_digit), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!newPassword.matches(".*[\\W].*".toRegex())){
                Toast.makeText(this, getString(R.string.info_password_need_special), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Appel de l'api pour modifier le mot de passe
            editPassword(oldPassword,newPassword)
        }
    }

    // Bloquer les interractions avec les composants et afficher la barre de progression
    fun waitForApi(ended: Boolean) {
        if (!ended) {
            logoutButton.isEnabled = false
            editUsernameInput.isEnabled = false
            editPasswordInput.isEnabled = false
            confirmPasswordInput.isEnabled = false
            oldPasswordInput.isEnabled = false
            saveUsernameButton.isEnabled = false
            savePasswordButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
        }
        else {
            logoutButton.isEnabled = true
            editUsernameInput.isEnabled = true
            editPasswordInput.isEnabled = true
            confirmPasswordInput.isEnabled = true
            oldPasswordInput.isEnabled = true
            saveUsernameButton.isEnabled = true
            savePasswordButton.isEnabled = true
            progressBar.visibility = View.INVISIBLE
        }
    }

    // Modifier le nom d'utilisateur
    private fun editUsername(newUsername : String) {
        waitForApi(false)
        lifecycleScope.launch {
            try {
                // Appel à l'API pour modifier l'utilisateur
                val response = RetrofitClient.instance.editUsername(ApiService.EditUsernameRequest(user.id,newUsername))
                val body = response.body()
                // Traitement des données
                if (body != null && body.statut == "success" && body.data != null) {
                    // Sauvegarde du nouveau nom d'utilisateur
                    user.name = newUsername
                    userDAO.init(this@UserSettingsActivity)
                    userDAO.open()
                    userDAO.update(user)
                    userDAO.close()
                    Toast.makeText(this@UserSettingsActivity, getString(R.string.info_username_changed), Toast.LENGTH_SHORT).show()
                    Log.i("UserSettingsActivity", "EditUsername Request Success - $newUsername")
                } else if (response.code() == 400 || response.code() == 404) {
                    Log.e("UserSettingsActivity", "EditUsername Request Error - ${response.body()?.message}")
                } else {
                    Log.e("UserSettingsActivity", "EditUsername Request Error - $response")
                }
            } catch (e: Exception) {
                Log.e("UserSettingsActivity", "EditUsername Request Error - $e")
            } finally {
                waitForApi(true)
            }
        }
    }

    // Modifier le mot de passe
    private fun editPassword(oldPassword : String, newPassword : String) {
        waitForApi(false)
        lifecycleScope.launch {
            try {
                // Appel à l'API pour modifier l'utilisateur
                val response = RetrofitClient.instance.editPassword(ApiService.EditPasswordRequest(user.id,oldPassword,newPassword))
                val body = response.body()
                // Traitement des données
                if (body != null && body.statut == "success" && body.data != null) {
                    // Sauvegarde du nouveau nom d'utilisateur
                    user.password = body.data.password
                    userDAO.init(this@UserSettingsActivity)
                    userDAO.open()
                    userDAO.update(user)
                    userDAO.close()
                    Toast.makeText(this@UserSettingsActivity, getString(R.string.info_password_changed), Toast.LENGTH_SHORT).show()
                    Log.i("UserSettingsActivity", "EditPassword Request Success - $newPassword")
                } else if (response.code() == 403) {
                    Toast.makeText(this@UserSettingsActivity, getString(R.string.error_old_password_not_matching), Toast.LENGTH_SHORT).show()
                    Log.e("UserSettingsActivity", "EditPassword Request Error - ${response.body()?.message}")
                } else if (response.code() == 417) {
                    Toast.makeText(this@UserSettingsActivity, getString(R.string.error_old_same_new_password), Toast.LENGTH_SHORT).show()
                } else if (response.code() == 400 || response.code() == 404) {
                    Log.e("UserSettingsActivity", "EditPassword Request Error - ${response.body()?.message}")
                } else {
                    Log.e("UserSettingsActivity", "EditPassword Request Error - $response")
                }
            } catch (e: Exception) {
                Log.e("UserSettingsActivity", "EditPassword Request Error - $e")
            } finally {
                waitForApi(true)
            }
        }
    }
}
