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
//import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : BaseActivity() {

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

    private var videoStarted = false
    private var currentYouTubePlayer: YouTubePlayer? = null

    private lateinit var currentMusicLayout : LinearLayout

    private lateinit var currentAuthor : TextView
    private lateinit var currentTitle : TextView
    private lateinit var currentDuration : TextView
    private lateinit var currentProposer : TextView
    private lateinit var currentLink : FloatingActionButton
    //private lateinit var progressBar : LinearProgressIndicator
    private lateinit var progressBarEditable : SeekBar

    private lateinit var currentPlayingMusic : Music
    private lateinit var musics : MutableList<Music>

    private lateinit var noMusicFound : Music

    private var isHost = false
    private var isAppInBackground = false
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private var currentRoomId = -1

    private lateinit var playerContainer : FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTracker.register(this)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars: Insets = insets.getInsets(Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
        //progressBar = findViewById(R.id.current_progress_spinner)
        progressBarEditable = findViewById(R.id.music_seek_bar)
        currentMusicLayout = findViewById(R.id.current_music_info)

        showVideoButton = findViewById(R.id.show_video_button)

        roomLayout = findViewById(R.id.room_layout)
        noRoomLayout = findViewById(R.id.no_room_layout)
        createRoomButton = noRoomLayout.findViewById<Button>(R.id.create_room_button)

        roomCode = findViewById(R.id.room_code)

        musicDAO.init(this)
        recyclerView = findViewById(R.id.next_music_list)
        adapter = MusicAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        progressSpinner = findViewById(R.id.progress_spinner)
        progressSpinner.visibility = View.GONE
        addMusicButton = findViewById(R.id.create_music_button)
        roomName = findViewById(R.id.room_name)


        var currentTotalTime = 1f
        var currentFormatedTotalTime = "00:00"
        lifecycle.addObserver(youTubePlayerView)
        youTubePlayerView.addYouTubePlayerListener(object : YouTubePlayerListener {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                currentYouTubePlayer = youTubePlayer
            }
            override fun onApiChange(youTubePlayer: YouTubePlayer) {}
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                //progressBar.max = currentTotalTime.toInt()
                progressBarEditable.max = currentTotalTime.toInt()
                //progressBar.progress = second.toInt()
                progressBarEditable.progress = second.toInt()
                val onePercent = (second / currentTotalTime) * 100
                if (onePercent >= 99f) {
                    //progressBar.progress = 0
                    progressBarEditable.progress = 0
                    handlePlaylist()
                }
                val formatedCurrentTime = String.format(Locale.getDefault(), "%d:%02d", second.toInt() / 60, second.toInt() % 60)
                val timeFormated = "$formatedCurrentTime / $currentFormatedTotalTime"
                currentDuration.text = timeFormated

            }
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {}
            override fun onPlaybackQualityChange(youTubePlayer: YouTubePlayer, playbackQuality: PlayerConstants.PlaybackQuality) {}
            override fun onPlaybackRateChange(youTubePlayer: YouTubePlayer, playbackRate: PlayerConstants.PlaybackRate) {}
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.VIDEO_CUED) {
                    youTubePlayer.play()
                }
            }
            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                val totalSeconds = duration.toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formatted = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                currentFormatedTotalTime = formatted
                currentDuration.text = formatted
                currentTotalTime = duration
            }
            override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {}
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

        noMusicFound =  Music(10000,"Music Title","Author Name","00:00","9QbudSq30bo",0,1,1,1,"Mr.Tester")
        currentPlayingMusic = noMusicFound
        musics = mutableListOf(currentPlayingMusic)

        playerContainer = findViewById<FrameLayout>(R.id.player_container)

        update()
    }

    private fun handlePlaylist(forward : Boolean = true){
        if (musics.isNotEmpty()) {
            if (musics.size > 1) {
                if (forward) {
                    musics.removeAt(0)
                    musics.add(currentPlayingMusic)
                    adapter.notifyItemMoved(0, musics.size - 1)
                } else {
                    musics.add(0, musics[musics.size - 1])
                    musics.removeAt(musics.size - 1)
                    adapter.notifyItemMoved(musics.size - 1, 0)
                }
            }
            currentPlayingMusic = musics[0]
            videoStarted = false
            displayMusic(currentPlayingMusic)
            playMusic()
        }else {
            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMusic(music: Music) {
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

    private fun playMusic() {
        currentYouTubePlayer?.unMute()
        currentYouTubePlayer?.setVolume(100)
        if (videoStarted) {
            currentYouTubePlayer?.play()
        } else {
            currentYouTubePlayer?.loadVideo(currentPlayingMusic.link, 0f)
            videoStarted = true
        }
    }

    private fun showSearchModal() {
        val dialog = BottomSheetDialog(this)
        val searchView = layoutInflater.inflate(R.layout.bottom_sheet_music_search, null)
        dialog.setContentView(searchView)
        val searchEditText = searchView.findViewById<EditText>(R.id.searchEditText)
        val searchButton = searchView.findViewById<Button>(R.id.searchButton)
        val recyclerViewSearch = searchView.findViewById<RecyclerView>(R.id.resultsRecyclerView)
        val progressSpinner = searchView.findViewById<ProgressBar>(R.id.progress_spinner_search)
        progressSpinner.visibility = View.GONE
        val searchAdapter = YouTubeResultAdapter(mutableListOf(), object : OnYouTubeVideoClickListener {
            override fun onYouTubeVideoClick(video: YouTubeVideoItem) {
                searchEditText.isEnabled = false
                searchEditText.isFocusable = false
                searchButton.isEnabled = false
                progressSpinner.visibility = View.VISIBLE
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

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitClient.instance.addMusic(ApiService.AddMusicRequest(musique.room_id, musique.user_id, musique.title, musique.artist, musique.link))
                        val body = response.body()
                        Log.d("YouTubeVideoDebug", "videoId=${video.id.videoId}, title=${video.snippet.title}")
                        if (body != null && body.statut == "success") {
                            withContext(Dispatchers.Main) {
                                musicDAO.insert(musique)
                                adapter.addItem(musique)
                                musics.add(musique)
                                adapter.notifyItemInserted(adapter.itemCount - 1)
                                Log.i("MainActivity", "AddMusic Request Success - $response")
                                dialog.dismiss()
                                if (musics.size == 1) {
                                    currentPlayingMusic = musique
                                    displayMusic(musique)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, getString(R.string.error_retry), Toast.LENGTH_SHORT).show()
                                Log.e("MainActivity", "AddMusic Request Error - $response")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("DB", "Erreur lors de l'ajout : ${e.message}")
                        }
                    } finally {
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
                        val items = body.items.filter { !it.id?.videoId.isNullOrEmpty() }
                        for (item in items) {
                            searchAdapter.addItem(item)
                            searchAdapter.notifyItemInserted(searchAdapter.itemCount - 1)
                        }
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

    private fun showPlayer() {
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

    private fun hidePlayer() {
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

    private fun displayRoomView(room: Room) {
        if (room.host_id != user.id) {
            isHost = false
            playButton.visibility = View.GONE
            pauseButton.visibility = View.GONE
            nextButton.visibility = View.GONE
            previousButton.visibility = View.GONE
            youTubePlayerView.visibility = View.GONE
            progressBarEditable.visibility = View.GONE
            currentMusicLayout.visibility = View.GONE
            //progressBar.visibility = View.GONE
        }
        else {
            isHost = true
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            nextButton.visibility = View.VISIBLE
            previousButton.visibility = View.VISIBLE
            youTubePlayerView.visibility = View.VISIBLE
            progressBarEditable.visibility = View.VISIBLE
            currentMusicLayout.visibility = View.VISIBLE
            //progressBar.visibility = View.GONE
        }
        roomName.text = room.label
        musicDAO.open()
        musics = musicDAO.index(room.id)
        roomCode.text = "Code : ${room.code}"
        adapter.clear()
        musics.forEach { music ->
            adapter.addItem(music)
            adapter.notifyItemInserted(adapter.itemCount - 1)
        }
        musicDAO.close()

        if (musics.isNotEmpty()) {
            currentPlayingMusic = musics.first()
            displayMusic(currentPlayingMusic)
        }else {
            displayMusic(noMusicFound)
            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT).show()
        }
        addMusicButton.setOnClickListener {
            showSearchModal()
        }
        showVideoButton.setOnClickListener {
            showPlayer()
        }
        playerContainer.setOnClickListener {
            hidePlayer()
        }
    }

    private fun displayNullView() {
        createRoomButton.setOnClickListener {
            val intent = Intent(this, CreateRoomActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    private fun update() {
        adapter.notifyDataSetChanged()
        val roomId = intent.getIntExtra("ROOM_ID", -1)
        currentRoomId = roomId
        val fetchedRoom = if (roomId != -1) roomDAO.get(roomId) else null
        if (fetchedRoom == null) {
            Log.w("MainActivity", "Room not found for ID: $roomId")
            displayNullView()
        } else {
            Log.i("MainActivity", "Room found: ${fetchedRoom.label}")
            displayRoomView(fetchedRoom)
        }
        val hasJoinedRoom = currentRoomId != -1
        if (hasJoinedRoom) {
            roomLayout.visibility = View.VISIBLE
            noRoomLayout.visibility = View.GONE
            progressSpinner.visibility = View.GONE
            addMusicButton.visibility = View.VISIBLE
        } else {
            roomLayout.visibility = View.GONE
            noRoomLayout.visibility = View.VISIBLE
            addMusicButton.visibility = View.GONE
            playerContainer.visibility = View.GONE
            progressSpinner.visibility = View.GONE
            currentMusicLayout.visibility = View.GONE
            createRoomButton.isEnabled = true
        }
    }

    fun getPollingInterval(): Long {
        return when {
            isHost && !isAppInBackground -> 15000L
            !isHost && !isAppInBackground -> 30000L
            isHost && isAppInBackground -> 30000L
            else -> -1L
        }
    }

    fun startPolling() {
        val interval = getPollingInterval()
        if (interval < 0) return

        pollingRunnable = object : Runnable {
            override fun run() {
                fetchRoomMusics()
                handler.postDelayed(this, interval)
            }
        }
        handler.post(pollingRunnable!!)
    }

    fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
    }

    fun fetchRoomMusics() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMusic(ApiService.GetMusicRequest(currentRoomId))
                val body = response.body()
                if (body != null && body.statut == "success" && body.data != null) {
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
                if (musics.size == 1) {
                    currentPlayingMusic = musics.first()
                    displayMusic(musics.first())
                }
            }
        }
    }

    override fun onDestroy() {
        //audioManager.abandonAudioFocus(focusChangeListener)
        super.onDestroy()
        ActivityTracker.unregister(this)
    }

    override fun onPause() {
        super.onPause()
        isAppInBackground = true
        stopPolling()
    }

    override fun onStop() {
        super.onStop()
        isAppInBackground = true
        stopPolling()
    }

    override fun onResume() {
        super.onResume()
        isAppInBackground = false
        update()
        if (currentRoomId != -1) {
            startPolling()
        }
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (playerContainer.alpha == 1f && !isTouchInsideView(
                ev, playerContainer
            )
        ) {
            hidePlayer()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    fun isTouchInsideView(ev: MotionEvent, view: View): Boolean {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(ev.rawX.toInt(), ev.rawY.toInt())
    }
}
