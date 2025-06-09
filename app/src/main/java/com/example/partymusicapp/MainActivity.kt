package com.example.partymusicapp

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partymusicapp.activity.BaseActivity
import com.example.partymusicapp.model.Music
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.support.ActivityTracker
import com.example.partymusicapp.support.MusicAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.activity.CreateRoomActivity
import com.example.partymusicapp.interfaces.ApiService
import com.example.partymusicapp.model.YouTubeVideoItem
import com.example.partymusicapp.support.Database.RetrofitClient
import com.example.partymusicapp.support.YouTubeAPI
import com.example.partymusicapp.support.YouTubeResultAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.partymusicapp.support.OnYouTubeVideoClickListener
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : BaseActivity() {

    // Déclaration des éléments de la vue
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MusicAdapter
    private lateinit var roomName: TextView
    private lateinit var progressSpinner: View
    private lateinit var addMusicButton : FloatingActionButton
    private lateinit var youTubePlayerView : YouTubePlayerView
    private lateinit var playButton : ImageButton
    private lateinit var pauseButton : ImageButton
    private lateinit var nextButton : ImageButton
    private lateinit var previousButton : ImageButton
    private lateinit var roomLayout : ScrollView
    private lateinit var noRoomLayout : FrameLayout
    private lateinit var createRoomButton : Button
    private lateinit var showVideoButton: FloatingActionButton
    private lateinit var roomCode : TextView
    private lateinit var currentMusicLayout : LinearLayout
    private lateinit var currentAuthor : TextView
    private lateinit var currentTitle : TextView
    private lateinit var currentDuration : TextView
    private lateinit var currentProposer : TextView
    private lateinit var currentLink : FloatingActionButton
    private lateinit var progressBarEditable : SeekBar
    private lateinit var playerContainer : FrameLayout

    // Déclaration des variables globales pour l'activity
    private var videoStarted = false
    private var currentYouTubePlayer: YouTubePlayer? = null
    private lateinit var currentPlayingMusic : Music
    private lateinit var musics : MutableList<Music>
    private lateinit var noMusicFound : Music
    private var isHost = false
    private var isAppInBackground = false
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var currentRoomId = -1

    // Création de la vue
    override fun onCreate(savedInstanceState: Bundle?) {
        // Contenu main
        super.onCreate(savedInstanceState)
        // suivre les activités ouvertes
        ActivityTracker.register(this)
        // afficher la vue
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars: Insets = insets.getInsets(Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Assignation des éléments de la vue
        playButton  = findViewById<ImageButton>(R.id.play_button)
        pauseButton = findViewById<ImageButton>(R.id.pause_button)
        nextButton = findViewById<ImageButton>(R.id.next_button)
        previousButton = findViewById<ImageButton>(R.id.previous_button)
        youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        currentAuthor = findViewById(R.id.current_author_name)
        currentTitle = findViewById(R.id.current_music_title)
        currentLink = findViewById(R.id.link_current_button)
        currentDuration = findViewById(R.id.current_duration)
        currentProposer = findViewById(R.id.current_proposer)
        progressBarEditable = findViewById(R.id.music_seek_bar)
        currentMusicLayout = findViewById(R.id.current_music_info)
        showVideoButton = findViewById(R.id.show_video_button)
        roomLayout = findViewById(R.id.room_layout)
        noRoomLayout = findViewById(R.id.no_room_layout)
        createRoomButton = noRoomLayout.findViewById<Button>(R.id.create_room_button)
        roomCode = findViewById(R.id.room_code)
        recyclerView = findViewById(R.id.next_music_list)
        addMusicButton = findViewById(R.id.create_music_button)
        progressSpinner = findViewById(R.id.progress_spinner)
        playerContainer = findViewById<FrameLayout>(R.id.player_container)

        // Assignation des variables globales
        musicDAO.init(this)
        roomDAO.init(this)
        adapter = MusicAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        progressSpinner.visibility = View.GONE
        roomName = findViewById(R.id.room_name)
        noMusicFound =  Music(10000,"Music Title","Author Name","00:00","9QbudSq30bo",0,1,1,1,"Mr.Tester")
        currentPlayingMusic = noMusicFound
        musics = mutableListOf(currentPlayingMusic)

        // Définition et assignation de variables locales
        var currentTotalTime = 1f
        var currentFormatedTotalTime = "00:00"

        // Définition du player
        lifecycle.addObserver(youTubePlayerView)
        // Gestion des évenements sur le player
        youTubePlayerView.addYouTubePlayerListener(object : YouTubePlayerListener {
            // Player chargé
            override fun onReady(youTubePlayer: YouTubePlayer) {
                // Assignation du player
                currentYouTubePlayer = youTubePlayer
            }
            // Lors d'un changement de source
            override fun onApiChange(youTubePlayer: YouTubePlayer) {}
            // Chaque secondes de video
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                // Mise à jour de la barre de progression
                progressBarEditable.max = currentTotalTime.toInt()
                progressBarEditable.progress = second.toInt()
                // Passer à la musique suivante si la fin est atteinte
                val onePercent = (second / currentTotalTime) * 100
                if (onePercent >= 99f) {
                    // Initialiser la prochaine musique
                    progressBarEditable.progress = 0
                    handlePlaylist()
                }
                // Affichage de la durée en texte
                val formatedCurrentTime = String.format(Locale.getDefault(), "%d:%02d", second.toInt() / 60, second.toInt() % 60)
                val timeFormated = "$formatedCurrentTime / $currentFormatedTotalTime"
                currentDuration.text = timeFormated
            }
            // Lorsqu'une erreur se produit
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {}
            // Lorsque la qualité de la vidéo change
            override fun onPlaybackQualityChange(youTubePlayer: YouTubePlayer, playbackQuality: PlayerConstants.PlaybackQuality) {}
            // Lorsque la vitesse de lecture change
            override fun onPlaybackRateChange(youTubePlayer: YouTubePlayer, playbackRate: PlayerConstants.PlaybackRate) {}
            // Lorsque il y a un changement dans la vidéo
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                // Si la vidéo est chargée, la jouer
                if (state == PlayerConstants.PlayerState.VIDEO_CUED) {
                    youTubePlayer.play()
                }
            }
            // Lorsque la durée de la vidéo change
            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                // Mise à jour de la durée totale
                val totalSeconds = duration.toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formatted = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                currentFormatedTotalTime = formatted
                currentDuration.text = formatted
                currentTotalTime = duration
            }
            // Lorsque la vidéo change
            override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {}
            // Lorsque une partie de la vidéo est chargée
            override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {}
        })
        progressBarEditable.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    var newProgress = (progress.toFloat() / 100) * currentTotalTime
                    currentYouTubePlayer?.seekTo(newProgress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        playButton.setOnClickListener {
            if (musics.isNotEmpty()) {
                playMusic()
                pauseButton.visibility = View.VISIBLE
                playButton.visibility = View.GONE
            }
        }
        pauseButton.setOnClickListener {
            currentYouTubePlayer?.pause()
            pauseButton.visibility = View.GONE
            playButton.visibility = View.VISIBLE
        }
        addMusicButton.setOnClickListener {
            showSearchModal()
        }
        nextButton.setOnClickListener {
            handlePlaylist(true)
        }
        previousButton.setOnClickListener {
            handlePlaylist(false)
        }

        // Mise à jour de la vue
        update()
    }

    // Gérer les actions sur les musiques
    private fun handlePlaylist(forward : Boolean = true){
        // Si il y a des musiques dans la room
        if (musics.isNotEmpty()) {
            // si il y a plus d'une musique
            if (musics.size > 1) {
                // Déplacer la musique
                if (forward) {
                    // Déplacer la musique du début à la fin
                    musics.removeAt(0)
                    musics.add(currentPlayingMusic)
                    adapter.notifyItemMoved(0, musics.size - 1)
                } else {
                    // Déplacer la musique de la fin au début
                    musics.add(0, musics[musics.size - 1])
                    musics.removeAt(musics.size - 1)
                    adapter.notifyItemMoved(musics.size - 1, 0)
                }
            }
            // Mettre à jour la musique du haut de la liste
            currentPlayingMusic = musics[0]
            videoStarted = false
            // afficher la musique
            displayMusic(currentPlayingMusic)
            // Jouer la musique
            playMusic()
        }else {
            // Si il n'y a pas de musique, afficher un message d'erreur
            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT).show()
        }
    }

    // Mettre à jour l'affichage de la musique
    private fun displayMusic(music: Music) {
        // Mettre à jour les informations de la musique
        currentAuthor.text = music.artist
        currentTitle.text = music.title
        currentDuration.text = music.duration
        val newText = getString(R.string.info_proposed) + " " + music.user_name
        currentProposer.text = newText
        currentLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://youtu.be/${music.link}".toUri())
            it.context.startActivity(intent)
        }
    }

    // Jouer la musique
    private fun playMusic() {
        // Remettre le volume à 100%
        currentYouTubePlayer?.unMute()
        currentYouTubePlayer?.setVolume(100)
        // Jouer la musique
        if (videoStarted) {
            // Si la vidéo a déjà été jouée, relancer la lecture
            currentYouTubePlayer?.play()
        } else {
            // Si la vidéo n'a pas encore été jouée, la charger
            currentYouTubePlayer?.loadVideo(currentPlayingMusic.link, 0f)
            videoStarted = true
        }
    }

    // Afficher le modal de recherche de musiques
    private fun showSearchModal() {
        // Définir le layout du modal
        val searchView = layoutInflater.inflate(R.layout.bottom_sheet_music_search, null)
        // Récupérer les éléments du modal
        val searchEditText = searchView.findViewById<EditText>(R.id.searchEditText)
        val searchButton = searchView.findViewById<Button>(R.id.searchButton)
        val recyclerViewSearch = searchView.findViewById<RecyclerView>(R.id.resultsRecyclerView)
        val progressSpinner = searchView.findViewById<ProgressBar>(R.id.progress_spinner_search)
        // Définir le modal
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(searchView)
        // Cacher la barre de progression
        progressSpinner.visibility = View.GONE
        // Définir la liste des musiques trouvées
        val searchAdapter = YouTubeResultAdapter(mutableListOf(), object : OnYouTubeVideoClickListener {
            // Quand on clique sur une musique
            override fun onYouTubeVideoClick(video: YouTubeVideoItem) {
                // Masquer le bouton de recherche et afficher la barre de progression
                searchEditText.isEnabled = false
                searchEditText.isFocusable = false
                searchButton.isEnabled = false
                progressSpinner.visibility = View.VISIBLE
                // Initialiser la musique
                val musique = Music(
                    id = musicDAO.getLastId(),
                    title = video.snippet.title,
                    artist = video.snippet.channelTitle,
                    link = video.id.videoId,
                    duration = "0",
                    likes = 0,
                    playable = 1,
                    user_id = userDAO.get()!!.id,
                    room_id = roomDAO.get(intent.getIntExtra("ROOM_ID", -1))!!.id,
                    user_name = userDAO.get()!!.name
                )
                // Appeler l'API pour ajouter la musique
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitClient.instance.addMusic(ApiService.AddMusicRequest(musique.room_id, musique.user_id, musique.title, musique.artist, musique.link))
                        val body = response.body()
                        Log.d("YouTubeVideoDebug", "videoId=${video.id.videoId}, title=${video.snippet.title}")
                        // Traitement des données
                        if (body != null && body.statut == "success") {
                            withContext(Dispatchers.Main) {  // <- comme c'est dans une coroutine, on doit utiliser withContext pour changer de thread et retourner sur le thread principal
                                // Ajouter la musique à la base de données et à la liste
                                musicDAO.insert(musique)
                                // Ajouter la musique à la liste
                                adapter.addItem(musique)
                                musics.add(musique)
                                adapter.notifyItemInserted(adapter.itemCount - 1)
                                Log.i("MainActivity", "AddMusic Request Success - $response")
                                // Fermer le modal
                                dialog.dismiss()
                                // Si il n'y a qu'une musique dans la liste, l'afficher
                                if (musics.size == 1) {
                                    currentPlayingMusic = musique
                                    displayMusic(musique)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                // Afficher un message d'erreur
                                Toast.makeText(this@MainActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                                Log.e("MainActivity", "AddMusic Request Error - $response")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("DB", "Erreur lors de l'ajout : ${e.message}")
                        }
                    } finally {
                        // Réactiver le bouton et masquer la barre de progression
                        withContext(Dispatchers.Main) {
                            searchEditText.isEnabled = true
                            searchEditText.isFocusableInTouchMode = true
                            searchButton.isEnabled = true
                            progressSpinner.visibility = View.GONE
                        }
                    }
                }
            }
        })
        // Définir la liste des musiques trouvées
        recyclerViewSearch.layoutManager = LinearLayoutManager(this)
        recyclerViewSearch.adapter = searchAdapter
        // Définir les actions sur le bouton de recherche
        searchButton.setOnClickListener {
            // Récupérer la recherche entrée
            val query = searchEditText.text.toString()
            // Bloquer le bouton et afficher la barre de progression
            searchEditText.isEnabled = false
            searchEditText.isFocusable = false
            searchButton.isEnabled = false
            progressSpinner.visibility = View.VISIBLE
            // Appeler l'API pour récupérer les musiques
            lifecycleScope.launch {
                try {
                    val response = YouTubeAPI.RetrofitClientYT.instanceYT.searchVideos(query = query)
                    val body = response.body()
                    // Traitement des données
                    if (body != null && body.items.isNotEmpty()) {
                        // Reinitialiser le texte
                        searchEditText.text.clear()
                        // Vider la liste des musiques trouvées
                        searchAdapter.clear()
                        searchAdapter.notifyDataSetChanged()
                        // Filtrer les musiques et retirer les chaines
                        val items = body.items.filter { !it.id?.videoId.isNullOrEmpty() }
                        // Ajouter les musiques trouvées à la liste
                        for (item in items) {
                            searchAdapter.addItem(item)
                            searchAdapter.notifyItemInserted(searchAdapter.itemCount - 1)
                        }
                    }
                    Log.d("YouTubeSearch", "Réponse de l'API: $body")
                } catch (e: Exception) {
                    Log.e("YouTubeSearch", "Erreur: ${e.message}")
                } finally {
                    // Réactiver le bouton et masquer la barre de progression
                    searchEditText.isEnabled = true
                    searchEditText.isFocusableInTouchMode = true
                    searchButton.isEnabled = true
                    progressSpinner.visibility = View.GONE
                }
            }
        }
        // Afficher le modal
        dialog.show()
    }

    // Afficher le player
    private fun showPlayer() {
        // Afficher lentement le player par dessus le reste
        playerContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .withStartAction {
                playerContainer.visibility = View.VISIBLE
                playerContainer.isClickable = true
                playerContainer.isFocusable = true
            }
            .start()
    }

    // Masquer le player
    private fun hidePlayer() {
        // Masquer lentement le player
        playerContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                playerContainer.isClickable = false
                playerContainer.isFocusable = false
                playerContainer.visibility = View.INVISIBLE
            }
            .start()
    }

    // Mettre à jour l'affichage de la salle
    private fun displayRoomView(room: Room) {
        // Définir les informations de la salle en fonction du rôle de l'utilisateur
        if (room.host_id != user.id) {
            // Si l'utilisateur n'est pas le host, masquer la musique en cours
            isHost = false
            playButton.visibility = View.GONE
            pauseButton.visibility = View.GONE
            nextButton.visibility = View.GONE
            previousButton.visibility = View.GONE
            youTubePlayerView.visibility = View.GONE
            progressBarEditable.visibility = View.GONE
            currentMusicLayout.visibility = View.GONE
        }
        else {
            // Si l'utilisateur est le host, afficher la musique en cours
            isHost = true
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            nextButton.visibility = View.VISIBLE
            previousButton.visibility = View.VISIBLE
            youTubePlayerView.visibility = View.VISIBLE
            progressBarEditable.visibility = View.VISIBLE
            currentMusicLayout.visibility = View.VISIBLE
        }
        // Mettre à jour les informations générales de la salle
        roomName.text = room.label
        roomCode.text = "Code : ${room.code}"
        // Récupérer les musiques de la salle
        musicDAO.open()
        musics = musicDAO.index(room.id)
        musicDAO.close()
        // Mettre à jour la liste des musiques
        adapter.clear()
        musics.forEach { music ->
            adapter.addItem(music)
            adapter.notifyItemInserted(adapter.itemCount - 1)
        }
        // Mettre à jour la musique en cours
        if (musics.isNotEmpty()) {
            currentPlayingMusic = musics.first()
            displayMusic(currentPlayingMusic)
        }else {
            // ou afficher un message d'erreur
            displayMusic(noMusicFound)
            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT).show()
        }
        // Définir les actions sur les boutons
        addMusicButton.setOnClickListener {
            // Afficher le modal de recherche de musiques
            showSearchModal()
        }
        showVideoButton.setOnClickListener {
            // Afficher le player
            showPlayer()
        }
        playerContainer.setOnClickListener {
            // Masquer le player
            hidePlayer()
        }
        // Commencer la mise à jour de la salle
        startPolling()
    }

    // Mettre à jour l'affichage en fonction des données
    private fun update() {
        // Mettre à jour la liste des musiques
        adapter.notifyDataSetChanged()
        // Récupérer la salle
        val roomId = intent.getIntExtra("ROOM_ID", -1)
        currentRoomId = roomId
        val fetchedRoom = if (roomId != -1) roomDAO.get(roomId) else null
        if (fetchedRoom == null) {
            // Arrêter la mise à jour de la salle
            stopPolling()
            // Afficher l'affichage d'accueil si aucune salle n'est fournie ou trouvée
            roomLayout.visibility = View.GONE
            noRoomLayout.visibility = View.VISIBLE
            addMusicButton.visibility = View.GONE
            playerContainer.visibility = View.GONE
            progressSpinner.visibility = View.GONE
            currentMusicLayout.visibility = View.GONE
            createRoomButton.isEnabled = true
            Log.w("MainActivity", "Room not found for ID: $roomId")
            createRoomButton.setOnClickListener {
                val intent = Intent(this, CreateRoomActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        } else {
            // Afficher l'affichage de la salle si une salle est trouvée
            roomLayout.visibility = View.VISIBLE
            noRoomLayout.visibility = View.GONE
            progressSpinner.visibility = View.GONE
            addMusicButton.visibility = View.VISIBLE
            Log.i("MainActivity", "Room found: ${fetchedRoom.label}")
            displayRoomView(fetchedRoom)
        }
    }

    // Définir l'intervalle de mise à jour de la salle
    fun getPollingInterval(): Long {
        return when {
            isHost && !isAppInBackground -> 15000L
            !isHost && !isAppInBackground -> 30000L
            isHost && isAppInBackground -> 30000L
            else -> -1L
        }
    }

    // Démarrer la mise à jour de la salle
    fun startPolling() {
        // récupérer l'intervalle de mise à jour
        val interval = getPollingInterval()
        if (interval < 0) return
        // Démarrer la mise à jour en fonction de l'intervalle
        pollingRunnable = object : Runnable {
            override fun run() {
                fetchRoomMusics()
                handler.postDelayed(this, interval)
            }
        }
        handler.post(pollingRunnable!!)
    }

    // Arrêter la mise à jour de la salle
    fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
    }

    // Appel à l'API pour récupérer les musiques de la salle
    fun fetchRoomMusics() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMusic(ApiService.GetMusicRequest(currentRoomId))
                val body = response.body()
                if (body != null && body.statut == "success" && body.data != null) {
                    // Traitement des données et mise à jour de la base de données et de la liste
                    val remoteMusics = body.data
                    musicDAO.open()
                    val localMusics = musicDAO.index(currentRoomId)
                    val remoteIds = remoteMusics.map { it.id }
                    val localIds = localMusics.map { it.id }
                    val musicsToAdd = remoteMusics.filter { it.id !in localIds }
                    for (music in musicsToAdd) {
                        musicDAO.insert(music)
                        adapter.addItem(music)
                        musics.add(music)
                        adapter.notifyItemInserted(adapter.itemCount - 1)
                    }
                    // Supprimer les musiques obsolètes
                    val musicsToRemove = localMusics.filter { it.id !in remoteIds }
                    for (music in musicsToRemove) {
                        musicDAO.delete(music.id)
                        adapter.removeItem(music)
                        musics.remove(music)
                        adapter.notifyItemRemoved(adapter.itemCount - 1)
                    }
                    musicDAO.close()
                    Log.i("MainActivity", "Sync complete: +${musicsToAdd.size}, -${musicsToRemove.size} for room $currentRoomId")

                } else if (response.code() == 404 || response.message() == "No musics found for this room") {
                    Log.e("MainActivity", "GetMusics Request Error - ${response.body()?.message}")
                    // Supprimer toutes les musiques de la salle et la mettre à jour
                    musicDAO.init(this@MainActivity)
                    musicDAO.open()
                    musicDAO.emptyRoom(currentRoomId)
                    musicDAO.close()
                } else {
                    Log.e("MainActivity", "GetMusics Request Error - $response")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "GetMusics Request Error - $e")
            } finally {
                // Mettre à jour la musique en cours
                if (musics.size == 1) {
                    currentPlayingMusic = musics.first()
                    displayMusic(musics.first())
                }
            }
        }
    }

    // Fermer l'activité
    override fun onDestroy() {
        super.onDestroy()
        ActivityTracker.unregister(this)
    }

    // Fermer l'activité et stopper la mise à jour
    override fun onPause() {
        super.onPause()
        isAppInBackground = true
        stopPolling()
    }

    // Fermer l'activité et Stoper la mise à jour
    override fun onStop() {
        super.onStop()
        isAppInBackground = true
        stopPolling()
    }

    // Démarrer la mise à jour
    override fun onResume() {
        super.onResume()
        isAppInBackground = false
        update()
        if (currentRoomId != -1) {
            startPolling()
        }
    }

    // Mettre à jour l'activité
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        update()
    }

    // Gérer le retour en arrière
    override fun onBackPressed() {
        if (ActivityTracker.isLastActivity()) {
            // Afficher une boite de dialogue pour confirmer la sortie
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_exit))
                .setMessage(getString(R.string.confirm_exit))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> finishAffinity() }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            // Fermer l'activité
            super.onBackPressed()
        }
    }

    // Gérer les interactions avec la vue lorsque le player est en mode plein écran
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (playerContainer.alpha == 1f && !isTouchInsideView(
                ev, playerContainer
            )
        ) {
            hidePlayer()
            return super.dispatchTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    // Vérifier si le point de touche est à l'intérieur de la vue
    fun isTouchInsideView(ev: MotionEvent, view: View): Boolean {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(ev.rawX.toInt(), ev.rawY.toInt())
    }
}
