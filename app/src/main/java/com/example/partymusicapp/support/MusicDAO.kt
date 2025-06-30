package com.example.partymusicapp.support

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.partymusicapp.model.Music
import com.example.partymusicapp.model.Playlist
import com.example.partymusicapp.support.PlaylistDAO

class MusicDAO {
    lateinit var base: SQLiteDatabase
    lateinit var localDatabase: LocalDatabase
    lateinit var playlistDAO: PlaylistDAO

    fun init(context: Context) {
        localDatabase = LocalDatabase(context)
        playlistDAO = PlaylistDAO()
        playlistDAO.init(context)

    }

    fun open() {
        base = localDatabase.writableDatabase
    }

    fun close() {
        base.close()
    }

    fun empty() {
        base.delete("music", null, null)
    }

    fun insert(music: Music): Boolean {
        open()
        val values = ContentValues()
        values.put("id", music.id)
        values.put("title", music.title)
        values.put("artist", music.artist)
        values.put("link", music.link)
        values.put("duration", music.duration)
        values.put("likes", music.likes)
        values.put("playable", music.playable)
        values.put("room_id", music.room_id)
        values.put("user_name", music.user_name)
        values.put("user_id", music.user_id)
        val result = base.insertWithOnConflict("music", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt()

        for (playlist in music.playlists) {
            playlistDAO.insert(playlist)
        }

        close()
        return result != -1
    }

    fun get(music_id: Int): Music? {
        open()
        val cursor = base.query("music", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                if (id == music_id) {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                    val link = cursor.getString(cursor.getColumnIndexOrThrow("link"))
                    val duration = cursor.getString(cursor.getColumnIndexOrThrow("duration"))
                    val likes = cursor.getInt(cursor.getColumnIndexOrThrow("likes"))
                    val playable = cursor.getInt(cursor.getColumnIndexOrThrow("playable"))
                    val room_id = cursor.getInt(cursor.getColumnIndexOrThrow("room_id"))
                    val user_name = cursor.getString(cursor.getColumnIndexOrThrow("user_name"))

                    val playlists = mutableListOf<Playlist>()
                    val playlistCursor = base.query("playlist", null, "room_id = ?", arrayOf(room_id.toString()), null, null, null)
                    if (playlistCursor.moveToFirst()) {
                        do {
                            val playlistId = playlistCursor.getInt(playlistCursor.getColumnIndexOrThrow("id"))
                            val playlistLabel = playlistCursor.getString(playlistCursor.getColumnIndexOrThrow("label"))
                            val playlist = Playlist(playlistId, playlistLabel, room_id)
                            playlists.add(playlist)
                        } while (playlistCursor.moveToNext())
                    }
                    playlistCursor.close()

                    val user_id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
                    cursor.close()
                    close()
                    return Music(id, title, artist, duration, link, likes, playable, user_id, room_id, playlists , user_name)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return null
    }

    fun index(room_id: Int): ArrayList<Music> {
        open()
        val musics = ArrayList<Music>()
        val cursor = base.query("music", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val roomIdInDB = cursor.getInt(cursor.getColumnIndexOrThrow("room_id"))
                if (roomIdInDB == room_id) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                    val link = cursor.getString(cursor.getColumnIndexOrThrow("link"))
                    val duration = cursor.getString(cursor.getColumnIndexOrThrow("duration"))
                    val likes = cursor.getInt(cursor.getColumnIndexOrThrow("likes"))
                    val playable = cursor.getInt(cursor.getColumnIndexOrThrow("playable"))
                    val user_name = cursor.getString(cursor.getColumnIndexOrThrow("user_name"))

                    val playlists = mutableListOf<Playlist>()

                    val playlistCursor = base.query(
                        "playlist", null, "room_id = ?", arrayOf(room_id.toString()), null, null, null
                    )
                    if (playlistCursor.moveToFirst()) {
                        do {
                            val playlistId = playlistCursor.getInt(playlistCursor.getColumnIndexOrThrow("id"))
                            val playlistLabel = playlistCursor.getString(playlistCursor.getColumnIndexOrThrow("label"))
                            val playlist = Playlist(playlistId, playlistLabel, room_id)
                            playlists.add(playlist)
                        } while (playlistCursor.moveToNext())
                    }
                    playlistCursor.close()



                    val user_id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
                    musics.add(Music(id, title, artist, duration, link, likes, playable, user_id, room_id, playlists , user_name))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return musics
    }

    fun emptyRoom(room_id: Int): Int {
        open()
        val deleted = base.delete("music", "room_id = ?", arrayOf(room_id.toString()))
        close()
        return deleted
    }

    fun getLastId(): Int {
        open()
        val cursor = base.query("music", null, null, null, null, null, null)
        var lastId = 0
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                if (id > lastId) {
                    lastId = id
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return lastId + 1
    }

    fun delete(music_id: Int) {
        open()
        base.delete("music", "id = ?", arrayOf(music_id.toString()))
        close()
    }

    fun deleteRoom(room_id: Int) {
        base.delete("music", "room_id = ?", arrayOf(room_id.toString()))
    }
}
