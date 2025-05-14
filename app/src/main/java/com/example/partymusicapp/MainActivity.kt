package com.example.partymusicapp

import android.R.attr.delay
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.partymusicapp.R
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.partymusicapp.support.OnYouTubeVideoClickListener


class MainActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MusicAdapter
    private lateinit var roomName: TextView
    private lateinit var progressSpinner: View
    private lateinit var addMusicButton : FloatingActionButton
    private lateinit var youTubePlayerView : YouTubePlayerView
    private lateinit var playButton : ImageButton
    private lateinit var pauseButton : ImageButton

    private lateinit var audioManager: AudioManager

    private val focusChangeListener: AudioManager.OnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> currentYouTubePlayer?.play()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> currentYouTubePlayer?.pause()
                AudioManager.AUDIOFOCUS_LOSS -> {
                    currentYouTubePlayer?.pause()
                    audioManager.abandonAudioFocus(focusChangeListener)
                }
            }
        }

    private var videoStarted = false
    private var currentYouTubePlayer: YouTubePlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTracker.register(this)

        setContentView(R.layout.activity_main)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            focusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "Audio focus granted")
        } else {
            Log.w("AudioFocus", "Audio focus NOT granted")
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars: Insets = insets.getInsets(Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        musicDAO.init(this)
        recyclerView = findViewById(R.id.next_music_list)
        adapter = MusicAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        progressSpinner = findViewById(R.id.progress_spinner)
        progressSpinner.visibility = View.GONE
        addMusicButton = findViewById(R.id.create_music_button)
        roomName = findViewById(R.id.room_name)
        update()

        playButton  = findViewById<ImageButton>(R.id.play_button)
        pauseButton = findViewById<ImageButton>(R.id.pause_button)

        youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : YouTubePlayerListener {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                currentYouTubePlayer = youTubePlayer
            }
            override fun onApiChange(youTubePlayer: YouTubePlayer) {}
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {}
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {}
            override fun onPlaybackQualityChange(youTubePlayer: YouTubePlayer, playbackQuality: PlayerConstants.PlaybackQuality) {}
            override fun onPlaybackRateChange(youTubePlayer: YouTubePlayer, playbackRate: PlayerConstants.PlaybackRate) {}
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.VIDEO_CUED) {
                    youTubePlayer.play()
                }
            }
            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {}
            override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {}
            override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {}

        })
        // play video
        playButton.setOnClickListener {
            playMusic(music = Music(10000,"Gagner un UHC Aleatoire","Guill","1:21:43","bY0tH3-kmG0",0,1,1,1,"Mr.Tester"))
        }
        // pause video
        pauseButton.setOnClickListener {
            currentYouTubePlayer?.pause()
        }

        // add music
        addMusicButton.setOnClickListener {
            showSearchModal()
        }
    }



    private fun showSearchModal() {
        val dialog = BottomSheetDialog(this)
        val searchView = layoutInflater.inflate(R.layout.bottom_sheet_music_search, null)
        dialog.setContentView(searchView)
        val searchEditText = searchView.findViewById<EditText>(R.id.searchEditText)
        val searchButton = searchView.findViewById<Button>(R.id.searchButton)
        val recyclerViewSearch = searchView.findViewById<RecyclerView>(R.id.resultsRecyclerView)
        val searchAdapter = YouTubeResultAdapter(mutableListOf(), object : OnYouTubeVideoClickListener {
            override fun onYouTubeVideoClick(video: YouTubeVideoItem) {
                val musique = Music(
                    id = musicDAO.getLastId(),
                    title = video.snippet.title,
                    artist = video.snippet.channelTitle,
                    link = "https://youtu.be/${video.id.videoId}",
                    duration = "0",
                    likes = 0,
                    playable = 1,
                    user_id = userDAO.get()!!.id,
                    room_id = roomDAO.get(intent.getIntExtra("ROOM_ID", -1))!!.id,
                    user_name = userDAO.get()!!.name)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitClient.instance.addMusic(ApiService.AddMusicRequest(musique.room_id, musique.user_id, musique.title, musique.artist, musique.link))
                        val body = response.body()
                        if (body != null && body.statut == "success") {
                            musicDAO.insert(musique)
                            adapter.addItem(musique)
                            adapter.notifyDataSetChanged()
                            dialog.hide()
                            dialog.dismiss()
                            Log.i("MainActivity", "AddMusic Request Success - $response")
                        } else {
                            Toast.makeText(this@MainActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                            Log.e("MainActivity", "AddMusic Request Error - $response")
                        }
                    } catch (e: Exception) {
                        Log.e("DB", "Erreur lors de l'ajout : ${e.message}")
                    }
                }
            }
        })
        val progressSpinner = searchView.findViewById<ProgressBar>(R.id.progress_spinner_search)
        progressSpinner.visibility = View.GONE
        recyclerViewSearch.layoutManager = LinearLayoutManager(this)
        recyclerViewSearch.adapter = searchAdapter
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            searchEditText.isEnabled = false
            searchEditText.isFocusable = false
            searchButton.isEnabled = false
            progressSpinner.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val response = YouTubeAPI.RetrofitClientYT.instanceYT.searchVideos(query = query)
                    val body = response.body()
                    if (body != null && body.items.isNotEmpty()) {
                        searchEditText.text.clear()
                        searchAdapter.clear()
                        for (item in body.items) {
                            searchAdapter.addItem(item)
                        }
                        searchAdapter.notifyDataSetChanged()
                    }
                    Log.d("YouTubeSearch", "Réponse de l'API: $body")
                } catch (e: Exception) {
                    Log.e("YouTubeSearch", "Erreur: ${e.message}")
                } finally {
                    searchEditText.isEnabled = true
                    searchEditText.isFocusableInTouchMode = true
                    searchButton.isEnabled = true
                    progressSpinner.visibility = View.GONE
                }
            }
        }
        dialog.show()
    }

    private fun playMusic(music: Music) {
        currentYouTubePlayer?.unMute()
        currentYouTubePlayer?.setVolume(100)
        if (videoStarted) {
            currentYouTubePlayer?.play()
        } else {
            currentYouTubePlayer?.loadVideo(music.link, 0f)
            videoStarted = true
        }
    }

    private fun displayRoomView(room: Room) {
        roomName.text = room.label
        musicDAO.open()
        val musics = musicDAO.index(room.id)
        adapter.clear()  // Clear previous music
        musics.forEach { music ->
            adapter.addItem(music)  // Add new music
        }
        adapter.notifyDataSetChanged()  // Force the adapter to refresh
        musicDAO.close()

        addMusicButton.setOnClickListener {
            showSearchModal()
        }
    }

    private fun displayNullView() {
        roomName.text = user.name
    }

    private fun update() {
        val roomId = intent.getIntExtra("ROOM_ID", -1)
        val fetchedRoom = if (roomId != -1) roomDAO.get(roomId) else null
        if (fetchedRoom == null) {
            Log.w("MainActivity", "Room not found for ID: $roomId")
            displayNullView()
        } else {
            Log.i("MainActivity", "Room found: ${fetchedRoom.label}")
            displayRoomView(fetchedRoom)
        }
    }

    override fun onDestroy() {
        audioManager.abandonAudioFocus(focusChangeListener)
        super.onDestroy()
        ActivityTracker.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        update()
    }

    override fun onBackPressed() {
        if (ActivityTracker.isLastActivity()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_exit))
                .setMessage(getString(R.string.confirm_exit))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> finishAffinity() }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
