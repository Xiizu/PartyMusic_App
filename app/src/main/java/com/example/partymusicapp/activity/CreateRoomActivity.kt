package com.example.partymusicapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.RoomDAO
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateRoomActivity : BaseActivity() {

    // initialisation des elements de la vue
    lateinit var label : EditText
    lateinit var description : EditText
    lateinit var createButton : FloatingActionButton
    lateinit var progressBars : ProgressBar

    // initialisation des variables globales pour l'activity
    override fun onCreate(savedInstanceState: Bundle?) {
        // Contenu create room
        super.onCreate(savedInstanceState)
        // suivre les activités ouvertes
        ActivityTracker.register(this)
        // afficher la vue
        setContentView(R.layout.activity_create_room)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Assignation des éléments de la vue
        description = findViewById<TextInputEditText>(R.id.description)
        label = findViewById<TextInputEditText>(R.id.label)
        createButton = findViewById<FloatingActionButton>(R.id.create_room_button)
        progressBars = findViewById(R.id.progress_spinner)

        // Modification de styles
        progressBars.visibility = ProgressBar.INVISIBLE
        description.movementMethod = ScrollingMovementMethod.getInstance()

        // activation du bouton si les champs de texte ne sont pas vides
        fun checkInputs(){
            val labelText = label.text.toString()
            createButton.isEnabled = labelText.isNotEmpty()
        }

        // lancer une vérification à chaque lettre tapée
        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        description.addTextChangedListener(inputWatcher)
        label.addTextChangedListener(inputWatcher)

        // Détecter le click sur le bouton de création de salle
        createButton.setOnClickListener {
            // Récupération des informations saisies
            val labelText = label.text.toString()
            val descriptionText = description.text.toString()
            val userId = user.id

            // Désactiver le bouton et afficher la barre de progression
            createButton.isEnabled = false
            progressBars.visibility = ProgressBar.VISIBLE

            // Appel à l'API pour la création de salle
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.createRoom(ApiService.CreateRoomRequest(id = userId, label = labelText, description = descriptionText))
                    val body = response.body()

                    // Réponse de l'API et traitement des données
                    if (body != null && body.statut == "success" && body.data != null) {
                        // Créer une nouvelle salle et l'ajouter à la base de données
                        val newRoom = body.data
                        val roomDAO = RoomDAO()
                        roomDAO.init(this@CreateRoomActivity)
                        roomDAO.insert(newRoom)
                        roomDAO.close()
                        // Démarrer l'activity principale et passer les données de la salle
                        startActivity(Intent(this@CreateRoomActivity, MainActivity::class.java).apply {
                            putExtra("ROOM_ID", newRoom.id)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                        shouldRecreate = true
                        finish()
                        Log.i("MainActivity", "CreateRoom Request Success -  $response")
                    } else if (response.code() == 433) {
                        // Afficher un message d'erreur si la salle est pleine
                        Toast.makeText(this@CreateRoomActivity,getString(R.string.info_max_room_reached),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "CreateRoom Request Error - $response")
                    } else if (response.code() == 400) {
                        // Afficher un message d'erreur
                        Toast.makeText(this@CreateRoomActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "CreateRoom Request Error - $response")
                    }
                } catch (e: Exception) {
                    // Afficher un message d'erreur
                    Toast.makeText(this@CreateRoomActivity,getString(R.string.error_retry),Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Login Request Error - $e")
                } finally {
                    // Réactiver le bouton et masquer la barre de progression
                    createButton.isEnabled = true
                    progressBars.visibility = ProgressBar.INVISIBLE
                }
            }
        }
    }

    // Gérer le retour en arrière
    override fun onDestroy() {
        super.onDestroy()
        ActivityTracker.unregister(this)
    }
}
