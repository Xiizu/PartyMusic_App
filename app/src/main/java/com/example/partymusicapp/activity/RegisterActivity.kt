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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RegisterActivity: AppCompatActivity() {

    // initialisation des elements de la vue
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordVerify: EditText
    private lateinit var button: Button
    private lateinit var progressBars: ProgressBar
    private lateinit var redirectLogin : TextView

    // initialisation des variables globales pour l'activity
    private var idUser : Int = 0
    private lateinit var nameUser: String
    private lateinit var emailUser: String
    private lateinit var tokenUser: String

    // Activation du bouton si les champs de texte sont valides
    fun checkInputs(){
        val nameText = name.text.toString()
        val emailText = email.text.toString()
        val passwordText = password.text.toString()
        val passwordVerifyText = passwordVerify.text.toString()
        button.isEnabled = emailText.isNotEmpty()
                && passwordText.isNotEmpty()
                && nameText.isNotEmpty()
                && passwordVerifyText.isNotEmpty()
                && validateInputs("password",passwordText).isEmpty()
                && validateInputs("passwordVerify",passwordVerifyText).isEmpty()
                && validateInputs("email",emailText).isEmpty()
                && validateInputs("name",nameText).isEmpty()
    }

    // Vérification des champs saisis par l'utilisateur
    fun validateInputs(type: String, value: String): List<String> {
        val errors = mutableListOf<String>()
        if (type == "name" && value.length < 3) {
            errors.add(getString(R.string.info_short_name))
        }
        if (type == "email" && !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            errors.add(getString(R.string.info_invalid_email))
        }
        if (type == "password" && value.length < 8) {
            errors.add(getString(R.string.info_password_need_8chars))
        }
        if (type == "password" && !value.matches(".*[A-Z].*".toRegex())) {
            errors.add(getString(R.string.info_password_need_upper))
        }
        if (type == "password" && !value.matches(".*[a-z].*".toRegex())) {
            errors.add(getString(R.string.info_password_need_lower))
        }
        if (type == "password" && !value.matches(".*[!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/`~\\\\-].*".toRegex())) {
            errors.add(getString(R.string.info_password_need_special))
        }
        if (type == "password" && !value.matches(".*[0-9].*".toRegex())) {
            errors.add(getString(R.string.info_password_need_digit))
        }
        if (type == "passwordVerify" && value != password.text.toString()) {
            errors.add(getString(R.string.info_confirm_different))
        }
        return errors
    }

    // Fonction de création de l'activity
    override fun onCreate(savedInstanceState: Bundle?) {
        // Contenu register
        super.onCreate(savedInstanceState)
        // suivre les activités ouvertes
        ActivityTracker.register(this)
        // afficher la vue
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Assignation des éléments de la vue
        progressBars = findViewById(R.id.progress_spinner)
        redirectLogin = findViewById(R.id.clickable_text_login)
        name = findViewById(R.id.input_name)
        email = findViewById(R.id.input_email)
        password = findViewById(R.id.input_password)
        passwordVerify = findViewById(R.id.input_password_verify)
        button = findViewById(R.id.button_register)
        progressBars = findViewById(R.id.progress_spinner)
        redirectLogin = findViewById(R.id.clickable_text_login)

        // Modification de styles
        progressBars.visibility = ProgressBar.INVISIBLE
        redirectLogin.paintFlags = redirectLogin.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        // lancer une vérification à chaque lettre tapée
        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        name.addTextChangedListener(inputWatcher)
        email.addTextChangedListener(inputWatcher)
        password.addTextChangedListener(inputWatcher)
        passwordVerify.addTextChangedListener(inputWatcher)

        // Vérification des champs saisis par l'utilisateur
        name.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val nameValue = name.text.toString()
                val errors = validateInputs("name",nameValue)
                if (errors.isNotEmpty()) {
                    name.error = errors.first()
                }
            }
        }
        email.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val emailValue = email.text.toString()
                val errors = validateInputs("email", emailValue)
                if (errors.isNotEmpty()) {
                    email.error = errors.first()
                }
            }
        }
        password.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val passwordValue = password.text.toString()
                val errors = validateInputs("password", passwordValue)
                if (errors.isNotEmpty()) {
                    password.error = errors.first()
                }
            }
        }
        passwordVerify.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val passwordVerifyValue = passwordVerify.text.toString()
                val errors = validateInputs("passwordVerify", passwordVerifyValue)
                if (errors.isNotEmpty()) {
                    passwordVerify.error = errors.first()
                }
            }
        }

        // Aller à l'activity de connexion
        redirectLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Détecter le click sur le bouton de création de compte
        button.setOnClickListener {
            // Récupération des informations saisies
            val givenEmail = email.text.toString()
            val givenPassword = password.text.toString()
            val givenName = name.text.toString()

            // Désactiver le bouton et afficher la barre de progression
            button.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            // Appel à l'API pour la création de compte
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registerUser(ApiService.RegisterRequest(name = givenName, email = givenEmail, password = givenPassword))
                    val body = response.body()

                    // Réponse de l'API et traitement des données
                    if (body != null && body.statut == "success" && body.data != null) {
                        // Créer un nouvel utilisateur et l'ajouter à la base de données
                        val newUser = body.data
                        val userDAO = UserDAO()
                        userDAO.init(this@RegisterActivity)
                        userDAO.insert(newUser)
                        // Récupération des informations de l'utilisateur
                        idUser = newUser.id
                        nameUser = newUser.name
                        emailUser = newUser.email
                        tokenUser = newUser.token
                        // Démarrer l'activity principale et passer les données de l'utilisateur
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
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
                        Toast.makeText(this@RegisterActivity, getString(R.string.info_register_success), Toast.LENGTH_SHORT).show()
                        Log.i("MainActivity", "Register Request Success - $newUser")
                    } else if (response.code() == 409) {
                        // Afficher un message d'erreur si l'email est déjà utilisé
                        Toast.makeText(this@RegisterActivity,getString(R.string.info_email_used),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Register Request Error - " + body?.message)
                    } else {
                        // Afficher un message d'erreur
                        Toast.makeText(this@RegisterActivity,getString(R.string.info_register_failed),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Register Request Error - " + body?.message)
                        Log.e("MainActivity", "Register Request Error - $response")
                    }
                } catch (e: Exception) {
                    // Afficher un message d'erreur
                    Toast.makeText(this@RegisterActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Register Request Error - $e")
                } finally {
                    // Réactiver le bouton et masquer la barre de progression
                    button.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                }
            }
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