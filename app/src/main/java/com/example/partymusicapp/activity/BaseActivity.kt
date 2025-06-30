package com.example.partymusicapp.activity

import android.content.Intent
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.MainActivity
import com.example.partymusicapp.R
import com.example.partymusicapp.support.RoomAdapter
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.model.User
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.MusicDAO
import com.example.partymusicapp.support.RoomDAO
import com.example.partymusicapp.support.UserDAO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import com.example.partymusicapp.support.PlaylistDAO
import com.example.partymusicapp.support.YouTubeAPI

open class BaseActivity : AppCompatActivity() {

    // initialisation des elements de la vue
    lateinit var drawerLayout: DrawerLayout
    lateinit var toolbar: Toolbar
    lateinit var titleText: TextView
    lateinit var settingButton: View
    lateinit var footerView: LinearLayout
    lateinit var refreshButton: ImageButton
    lateinit var searchBar: TextInputEditText
    lateinit var roomRecycler: RecyclerView
    lateinit var roomAdapter: RoomAdapter
    lateinit var progressBarDrawer : ProgressBar

    // initialisation des variables globales pour l'activity
    val userDAO = UserDAO()
    lateinit var user: User
    val roomDAO = RoomDAO()
    lateinit var rooms: ArrayList<Room>
    val musicDAO = MusicDAO()
    // var currentRoom: Room? = null
    val playlistDAO = PlaylistDAO()

    // afficher la vue
    override fun setContentView(layoutResID: Int) {
        // Définition de la barre d'état
        val drawer = layoutInflater.inflate(R.layout.activity_base, null)
        val container = drawer.findViewById<FrameLayout>(R.id.base_content)
        // Ajout du menu latéral
        layoutInflater.inflate(layoutResID, container, true)
        super.setContentView(drawer)

        // Modifier la couleur de la barre d'état
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        // Assignation de la variable contenant la liste des salles
        rooms = ArrayList()

        // Assignation des éléments de la vue
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        titleText = findViewById(R.id.header_title)
        searchBar = findViewById(R.id.search_bar)
        settingButton = findViewById(R.id.button_setting)
        roomRecycler = findViewById(R.id.room_holder)
        refreshButton = findViewById(R.id.refresh_button)
        footerView = findViewById(R.id.footer)
        progressBarDrawer = findViewById(R.id.progress_spinner_drawer)

        // Initialisation du menu latéral
        if (setupDrawer()) {
            // Gestion de la barre de recherche
            var isEditing = false
            searchBar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isEditing) {
                        isEditing = true
                        val upperText = s.toString().uppercase(Locale.getDefault())
                        searchBar.setText(upperText)
                        searchBar.setSelection(upperText.length)
                        isEditing = false
                    }
                }
            })
            // Lancement de l'action de recherche
            searchBar.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    val query = searchBar.text.toString()
                    if (query.length == 8) {
                        joinRoom(query)
                    }
                    true
                } else false
            }
        }
    }

    // Redirection vers la salle
    fun goToRoom(room: Room) {
        // Afficher la barre de progression
        progressBarDrawer.visibility = View.VISIBLE
        // Envoi vers la salle
        lifecycleScope.launch {
            // Récupération des musiques de la salle
            fetchMusics(room.id)
            // Envoi vers l'activity de la salle et passage des données de la salle
            startActivity(Intent(this@BaseActivity, MainActivity::class.java).apply {
                putExtra("ROOM_ID", room.id)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            // Masquage de la barre de progression
            progressBarDrawer.visibility = View.GONE
        }
    }

    // Récupération des musiques de la salle
    private suspend fun fetchMusics(roomId: Int) {
        try {
            // Appel à l'API pour récupérer les musiques de la salle
            val response = RetrofitClient.instance.getMusic(ApiService.GetMusicRequest(roomId))
            val body = response.body()
            // Traitement des données
            if (body != null && body.statut == "success" && body.data != null) {
                // Génération de la salle mise à jour dans la base de données
                musicDAO.init(this@BaseActivity)
                musicDAO.open()
                musicDAO.emptyRoom(roomId)
                for (music in body.data) {
                    musicDAO.insert(music)
                }
                musicDAO.close()
                Log.i(
                    "MainActivity",
                    "GetMusics Request Success - ${body.data.size} music fetched for room $roomId"
                )
            } else if (response.code() == 404 || response.message() == "No musics found for this room") {
                Log.e("MainActivity", "GetMusics Request Error - ${response.body()?.message}")
                // Vide la salle si elle est vide
                musicDAO.init(this@BaseActivity)
                musicDAO.open()
                musicDAO.emptyRoom(roomId)
                musicDAO.close()
            } else {
                Log.e("MainActivity", "GetMusics Request Error - $response")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "GetMusics Request Error - $e")
        }
    }

    // Récupération des salles de l'utilisateur
    private fun fetchRooms() {
        // Afficher la barre de progression
        progressBarDrawer.visibility = View.VISIBLE
        // Appel à l'API pour récupérer les salles de l'utilisateur
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserRoom(ApiService.GetUserRoomRequest(user.id))
                val body = response.body()
                // Traitement des données
                if (body != null && body.statut == "success" && body.data != null) {
                    // Génération des salles de l'utilisateur dans la base de données
                    roomDAO.init(this@BaseActivity)
                    roomDAO.open()
                    roomDAO.empty()
                    for (room in body.data) {
                        roomDAO.insert(room)
                    }
                    roomDAO.close()
                    Log.i("MainActivity", "GetUserRoom Request Success - ${body.data.size} room fetched")
                } else if (response.code() == 400 || response.code() == 404) {
                    Log.e("MainActivity", "GetUserRoom Request Error - ${response.body()?.message}")
                } else {
                    Log.e("MainActivity", "GetUserRoom Request Error - $response")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "GetUserRoom Request Error - $e")
            } finally {
                // Récupération des salles de l'utilisateur dans la base de données
                roomDAO.open()
                val roomsToAdd = roomDAO.index()
                roomDAO.close()
                // Mise à jour de la liste des salles
                rooms.clear()
                rooms.addAll(roomsToAdd)
                roomAdapter.updateRooms(rooms)
                roomAdapter.notifyDataSetChanged()
                // Masquage de la barre de progression
                progressBarDrawer.visibility = View.GONE
            }
        }
    }

    // Initialisation de l'API Youtube pour générer une session
    private fun initYoutubeApi() {
        // Appel à l'API pour initialiser la session
        lifecycleScope.launch {
            try {
                val response = YouTubeAPI.RetrofitClientYT.instanceYT.initApiRequest()
                Log.i("BaseActivity", "Youtube API Request init")
            } catch (e: Exception) {
                Log.e("BaseActivity", "Youtube API Request Error - $e")
            } finally {
                Log.i("BaseActivity", "Youtube API Request ended")
            }
        }
    }

    // Vérification de l'existence de l'utilisateur
    private fun checkUser(): Boolean {
        // Récupération de l'utilisateur connecté
        userDAO.init(this)
        val connectedUser = userDAO.get()
        // Vérification de l'existence de l'utilisateur
        if (connectedUser == null) {
            // Redirection vers la page d'inscription si aucun utilisateur n'est trouvé
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return false
        } else {
            user = connectedUser
            return true
        }
    }

    // Initialisation du menu latéral
    private fun setupDrawer(): Boolean {
        // Vérification de la connexion de l'utilisateur
        if (!checkUser()) {
            return false
        }
        // Génération du menu latéral
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.info_drawer_oppened,
            R.string.info_drawer_closed
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // Génération de la liste des salles
        roomRecycler.layoutManager = LinearLayoutManager(this)
        roomAdapter = RoomAdapter(this@BaseActivity, rooms)
        roomRecycler.adapter = roomAdapter
        // Modification de styles
        titleText.text = user.name
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val iconColor = if (isDarkTheme) R.color.white else R.color.black
        toggle.drawerArrowDrawable.color = ResourcesCompat.getColor(resources, iconColor, null)
        // Détection du click sur la toolbar
        toolbar.setOnClickListener {
            // Redirection vers la page d'accueil
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        // Détection du click sur le bouton de réglages
        settingButton.setOnClickListener {
            // Redirection vers la page de réglages
            val intent = Intent(this, UserSettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
        // Détection du click sur le bouton de création de salle
        footerView.setOnClickListener {
            // Redirection vers la page de création de salle
            if (this::class != CreateRoomActivity::class) {
                val intent = Intent(this, CreateRoomActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }
        // Détection du click sur le bouton de rafraichissement de la liste des salles
        refreshButton.setOnClickListener {
            // Rafraîchissement de la liste des salles
            fetchRooms()
            titleText.text = user.name
        }
        // Récupération des salles de l'utilisateur
        fetchRooms()
        // Initialisation de l'API Youtube
        initYoutubeApi()
        return true
    }

    // Rejoindre une nouvelle salle
    private fun joinRoom(code: String) {
        // Désactiver la barre de recherche
        searchBar.clearFocus()
        searchBar.isEnabled = false

        // Appel à l'API pour rejoindre la salle
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.joinRoom(ApiService.JoinRoomRequest(user.id, code))
                val body = response.body()
                // Traitement des données
                if (body != null && body.statut == "success" && body.data != null) {
                    // Réinitialisation de la barre de recherche
                    searchBar.text = null
                    // Génération de la salle dans la base de données
                    val joinedRoom = body.data
                    roomDAO.open()
                    roomDAO.insert(joinedRoom)
                    roomDAO.close()
                    // Mise à jour de la liste des salles
                    rooms.add(joinedRoom)
                    roomAdapter.updateRooms(rooms)
                    roomAdapter.notifyDataSetChanged()
                    // Récupération des musiques de la salle et redirection vers la salle
                    lifecycleScope.launch {
                        fetchMusics(joinedRoom.id)
                        goToRoom(joinedRoom)
                    }
                    Log.i("MainActivity", "JoinRoom Request Success - $response")
                } else if (response.code() == 400 || response.code() == 404) {
                    // Afficher un message d'erreur si la salle n'existe pas ou si l'utilisateur est déjà dans la salle
                    Toast.makeText(this@BaseActivity, getString(R.string.error_room_not_found), Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "JoinRoom Request Error - ${body?.message}")
                } else {
                    // Afficher un message d'erreur
                    Toast.makeText(this@BaseActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "JoinRoom Request Unknown Error - $response")
                }
            } catch (e: Exception) {
                // Afficher un message d'erreur
                Toast.makeText(this@BaseActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "JoinRoom Request Exception - $e")
            } finally {
                // Réactiver la barre de recherche
                searchBar.isEnabled = true
            }
        }
    }

    // Gérer le retour en arrière
    override fun onBackPressed() {
        // Si c'est la dernière activité, afficher une boite de dialogue de confirmation
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

    // Redéfinition de la méthode onResume pour fermer le menu
    override fun onResume() {
        super.onResume()
        // Rafraîchir la liste des salles
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        if (shouldRecreate) {
            shouldRecreate = false
            recreate()
        }
    }

    // Objet de gestion des activités ouvertes
    companion object {
        var shouldRecreate = false
    }

    // Cacher le clavier lorsque l'utilisateur clique en dehors de celui ci
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus!!.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

}
