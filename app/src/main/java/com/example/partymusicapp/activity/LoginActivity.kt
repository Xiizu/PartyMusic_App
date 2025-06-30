package com.example.partymusicapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.partymusicapp.R
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.RoomDAO
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(){

    // initialisation des elements de la vue
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var button: Button
    private lateinit var progressBars: ProgressBar
    private lateinit var redirectRegister : TextView

    // initialisation des variables globales pour l'activity
    private var idUser : Int = 0
    private lateinit var nameUser: String
    private lateinit var emailUser: String
    private lateinit var tokenUser: String

    // Activation du bouton si les champs de texte ne sont pas vides
    fun checkInputs(){
        val emailText = email.text.toString()
        val passwordText = password.text.toString()
        button.isEnabled = emailText.isNotEmpty() && passwordText.isNotEmpty()
    }

    // Fonction de création de l'activity
    override fun onCreate(savedInstanceState: Bundle?) {
        // Contenu login
        super.onCreate(savedInstanceState)
        // suivre les activités ouvertes
        ActivityTracker.register(this)
        // afficher la vue
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Assignation des éléments de la vue
        progressBars = findViewById(R.id.progress_spinner)
        redirectRegister = findViewById(R.id.clickable_text_register)
        email = findViewById(R.id.input_email)
        password = findViewById(R.id.input_password)
        button = findViewById(R.id.button_login)

        // Modification de styles
        progressBars.visibility = ProgressBar.INVISIBLE
        redirectRegister.paintFlags = redirectRegister.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        // lancer une vérification à chaque lettre tapée
        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        email.addTextChangedListener(inputWatcher)
        password.addTextChangedListener(inputWatcher)

        // Détecter le click sur le bouton de connexion
        button.setOnClickListener {
            // Récupération des informations saisies
            val givenEmail = email.text.toString()
            val givenPassword = password.text.toString()

            // Désactiver le bouton et afficher la barre de progression
            button.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            // Appel à l'API pour la connexion
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.loginUser(ApiService.LoginRequest(email = givenEmail,password = givenPassword))
                    val body = response.body()

                    // Réponse de l'API et traitement des données
                    if (body != null && body.statut == "success" && body.data != null) {
                        // Créer un nouvel utilisateur et l'ajouter à la base de données
                        val newUser = body.data
                        val userDAO = UserDAO()
                        userDAO.init(this@LoginActivity)
                        userDAO.insert(newUser)
                        // Récupération des informations de l'utilisateur
                        idUser = newUser.id
                        nameUser = newUser.name
                        emailUser = newUser.email
                        tokenUser = newUser.token
                        // Démarrer l'activity principale et passer les données de l'utilisateur
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("id", idUser)
                            putExtra("email", emailUser)
                            putExtra("name", nameUser)
                            putExtra("token", tokenUser)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        setResult(RESULT_OK, intent)
                        startActivity(intent)
                        finish()
                        // Afficher un message de succès
                        Toast.makeText(this@LoginActivity,getString(R.string.info_login_success),Toast.LENGTH_SHORT).show()
                        Log.i("MainActivity", "Login Request Success - $newUser")
                    } else {
                        // Afficher un message d'erreur
                        Toast.makeText(this@LoginActivity,getString(R.string.info_login_failed),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Login Request Error - $response")
                    }
                } catch (e: Exception) {
                    // Afficher un message d'erreur
                    Toast.makeText(this@LoginActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Login Request Error - $e")
                } finally {
                    // Réactiver le bouton et masquer la barre de progression
                    button.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                }
            }
        }

        // Aller à l'activity de création de compte
        redirectRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Gérer le retour en arrière
    override fun onBackPressed() {
        // Si c'est la dernière activité, afficher une boite de dialogue de confirmation avant de quitter
        if (ActivityTracker.isLastActivity()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_exit))
                .setMessage(getString(R.string.confirm_exit))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> finishAffinity() }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            // Sinon, revenir à l'activité précédente
            super.onBackPressed()
        }
    }
}
