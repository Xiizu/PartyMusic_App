package com.example.partymusicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.R
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.Database.RetrofitClient
import kotlinx.coroutines.launch
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.partymusicapp.activity.LoginActivity
import com.example.partymusicapp.model.User
import com.example.partymusicapp.support.UserDAO


class MainActivity : AppCompatActivity() {

    val userDAO = UserDAO()
    lateinit var user : User

    lateinit var username : TextView
    lateinit var logout : Button

//    val loginActivity =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//                result : ActivityResult ->
//            if(result.resultCode == RESULT_OK) {
//                val data = result.data
//                if (data != null) {
//                    user = User(
//                        data.getIntExtra("id", 0),
//                        data.getStringExtra("name").toString(),
//                        data.getStringExtra("email").toString(),
//                        data.getStringExtra("password").toString(),
//                        data.getStringExtra("token").toString(),
//                    )
//                }
//            }
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Vérifier si un utilisateur est déjà connecté
        userDAO.init(this)
        val connectedUser = userDAO.get()
        if (connectedUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            user = connectedUser
        }

        // Menu principal
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logout = findViewById(R.id.logout)
        logout.setOnClickListener {
            userDAO.open()
            userDAO.empty()
            userDAO.close()
            recreate()
        }

















//        lifecycleScope.launch {
//            try {
//                val response = RetrofitClient.instance.getPing()
//                val body = response.body()
//                if (body?.message == null) {
//                    Log.e("MainActivity", "LOG - Error no message set; Body : " + body.toString() + "; Response : " + response.toString() + ";")
//                    runOnUiThread {
//                        Toast.makeText(this@MainActivity, "LOG - Error no message set", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    Log.i("MainActivity", "LOG - " + response.body()?.message.toString())
//                    runOnUiThread {
//                        Toast.makeText (this@MainActivity, "LOG - " + response.body()?.message, Toast.LENGTH_LONG).show()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("MainActivity", "LOG - " + e.toString())
//                runOnUiThread {
//                    Toast.makeText (this@MainActivity, "LOG - " + e.toString(), Toast.LENGTH_LONG).show()
//                }
//            }
//        }



    }
}
