package com.example.partymusicapp

import android.os.Bundle
import android.util.Log
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


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPing()
                val body = response.body()
                if (body?.message == null) {
                    Log.e("MainActivity", "LOG - Error no message set; Body : " + body.toString() + "; Response : " + response.toString() + ";")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "LOG - Error no message set", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.i("MainActivity", "LOG - " + response.body()?.message.toString())
                    runOnUiThread {
                        Toast.makeText (this@MainActivity, "LOG - " + response.body()?.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "LOG - " + e.toString())
                runOnUiThread {
                    Toast.makeText (this@MainActivity, "LOG - " + e.toString(), Toast.LENGTH_LONG).show()
                }
            }
        }

    }
}