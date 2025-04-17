package com.example.partymusicapp.activity

import android.app.Activity
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
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RegisterActivity: AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordVerify: EditText
    private lateinit var button: Button
    private lateinit var progressBars: ProgressBar
    private lateinit var redirectLogin : TextView

    private var idUser : Int = 0
    private lateinit var nameUser: String
    private lateinit var emailUser: String
    private lateinit var tokenUser: String

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
        if (type == "password" && !value.matches(".*[!@#\$%^&*()_+=\\[\\]{}|;:'\",.<>?/`~\\\\-].*".toRegex())) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        progressBars = findViewById(R.id.progress_spinner)
        redirectLogin = findViewById(R.id.clickable_text_login)
        progressBars.visibility = ProgressBar.INVISIBLE
        redirectLogin.setPaintFlags(redirectLogin.getPaintFlags() or android.graphics.Paint.UNDERLINE_TEXT_FLAG)

        name = findViewById(R.id.input_name)
        email = findViewById(R.id.input_email)
        password = findViewById(R.id.input_password)
        passwordVerify = findViewById(R.id.input_password_verify)
        button = findViewById(R.id.button_register)
        progressBars = findViewById(R.id.progress_spinner)
        redirectLogin = findViewById(R.id.clickable_text_login)



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

        redirectLogin.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        button.setOnClickListener {
            val givenEmail = email.text.toString()
            val givenPassword = password.text.toString()
            val givenName = name.text.toString()

            button.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registerUser(
                        ApiService.RegisterRequest(
                            name = givenName,
                            email = givenEmail,
                            password = givenPassword
                        )
                    )
                    val body = response.body()

                    if (body != null && body.statut == "success" && body.data != null) {
                        val newUser = body.data
                        val userDAO = UserDAO()
                        userDAO.init(this@RegisterActivity)
                        userDAO.insert(newUser)

                        idUser = newUser.id
                        nameUser = newUser.name
                        emailUser = newUser.email
                        tokenUser = newUser.token

                        val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
                            putExtra("id", idUser)
                            putExtra("email", emailUser)
                            putExtra("name", nameUser)
                            putExtra("token", tokenUser)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        setResult(Activity.RESULT_OK, intent)
                        startActivity(intent)
                        finish()

                        Toast.makeText(this@RegisterActivity,getString(R.string.info_register_success),Toast.LENGTH_SHORT).show()
                        Log.i("MainActivity", "Register Request Success - " + newUser.toString())
                    } else {
                        Toast.makeText(this@RegisterActivity,getString(R.string.info_register_failed),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Register Request Error - " + body?.message)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Register Request Error - " + e.toString())
                } finally {
                    button.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                }
            }
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_exit))
            .setMessage(getString(R.string.confirm_exit))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}