package com.example.partymusicapp.support


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.partymusicapp.model.Playlist
import com.example.partymusicapp.model.Room

class PlaylistDAO {
    lateinit var base: SQLiteDatabase
    lateinit var localDatabase: LocalDatabase

    fun init(context: Context) {
        localDatabase = LocalDatabase(context)
    }

    fun open() {
        base = localDatabase.writableDatabase
    }

    fun close() {
        base.close()
    }

    fun empty() {
        base.delete("playlist", null, null)
    }

    fun insert(playlist: Playlist): Boolean {
        open()
        val values = ContentValues()
        values.put("id", playlist.id)
        values.put("label", playlist.label)
        values.put("room_id", playlist.room_id)
        val result = base.insertWithOnConflict("playlist", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt()
        close()
        return result != -1
    }

    fun get(room : Room) : MutableList<Playlist> {
        open()
        val cursor = base.rawQuery("SELECT * FROM playlist WHERE room_id = ${room.id}", null)

        val playlists = mutableListOf<Playlist>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
                val room_id = cursor.getInt(cursor.getColumnIndexOrThrow("room_id"))
                playlists.add(Playlist(id, label, room_id))
            } while (cursor.moveToNext())
        }
        close()
        return playlists
    }


}