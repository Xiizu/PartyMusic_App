package com.example.partymusicapp.activity

import android.annotation.SuppressLint
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
import com.example.partymusicapp.R
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(){

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var button: Button
    private lateinit var progressBars: ProgressBar
    private lateinit var redirectRegister : TextView

    private var idUser : Int = 0
    private lateinit var nameUser: String
    private lateinit var emailUser: String
    private lateinit var tokenUser: String

    fun checkInputs(){
        val emailText = email.text.toString()
        val passwordText = password.text.toString()
        button.isEnabled = emailText.isNotEmpty() && passwordText.isNotEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBars = findViewById(R.id.progress_spinner)
        redirectRegister = findViewById(R.id.clickable_text_register)
        progressBars.visibility = ProgressBar.INVISIBLE
        redirectRegister.setPaintFlags(redirectRegister.getPaintFlags() or android.graphics.Paint.UNDERLINE_TEXT_FLAG)

        email = findViewById(R.id.input_email)
        password = findViewById(R.id.input_password)
        button = findViewById(R.id.button_login)

        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        email.addTextChangedListener(inputWatcher)
        password.addTextChangedListener(inputWatcher)

        button.setOnClickListener {
            val givenEmail = email.text.toString()
            val givenPassword = password.text.toString()

            button.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.loginUser(
                        ApiService.LoginRequest(
                            email = givenEmail,
                            password = givenPassword
                        )
                    )
                    val body = response.body()

                    if (body != null && body.statut == "success" && body.data != null) {
                        val newUser = body.data
                        val userDAO = UserDAO()
                        userDAO.init(this@LoginActivity)
                        userDAO.insert(newUser)

                        idUser = newUser.id
                        nameUser = newUser.name
                        emailUser = newUser.email
                        tokenUser = newUser.token

                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("id", idUser)
                            putExtra("email", emailUser)
                            putExtra("name", nameUser)
                            putExtra("token", tokenUser)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        setResult(Activity.RESULT_OK, intent)
                        startActivity(intent)
                        finish()

                        Toast.makeText(this@LoginActivity,getString(R.string.info_login_success),Toast.LENGTH_SHORT).show()
                        Log.i("MainActivity", "Login Request Success - " + newUser.toString())
                    } else {
                        Toast.makeText(this@LoginActivity,getString(R.string.info_login_failed),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Login Request Error - " + body?.message)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Login Request Error - " + e.toString())
                } finally {
                    button.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                }
            }
        }

        redirectRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_exit))
            .setMessage(getString(R.string.confirm_exit))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> finishAffinity() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
