package com.example.partymusicapp.support

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.partymusicapp.model.Room

class RoomDAO {
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
        base.delete("room", null, null)
    }

    fun insert(room: Room) : Boolean {
        open()
        val values = ContentValues()
        values.put("id", room.id)
        values.put("label", room.label)
        values.put("description", room.description)
        values.put("code", room.code)
        values.put("host_id", room.host_id)
        values.put("host_name", room.host_name)
        val result = base.insertWithOnConflict("room", null, values, SQLiteDatabase.CONFLICT_REPLACE).toInt() // enregistrer la room dans la base de données
        close()
        return result != -1
    }

    fun get(room_id : Int) : Room? {
        open()
        val cursor = base.query("room", null, null, null, null, null, null) // Récupérer les room existantes
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                if (id == room_id) {
                    val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
                    val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                    val code = cursor.getString(cursor.getColumnIndexOrThrow("code"))
                    val host_id = cursor.getInt(cursor.getColumnIndexOrThrow("host_id"))
                    val host_name = cursor.getString(cursor.getColumnIndexOrThrow("host_name"))
                    cursor.close()
                    close()
                    return Room(id, label, description, code, host_id, host_name)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return null
    }

    fun index() : ArrayList<Room> {
        open()
        val cursor = base.query("room", null, null, null, null, null, null) // Récupérer les room existantes
        val rooms = ArrayList<Room>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val code = cursor.getString(cursor.getColumnIndexOrThrow("code"))
                val host_id = cursor.getInt(cursor.getColumnIndexOrThrow("host_id"))
                val host_name = cursor.getString(cursor.getColumnIndexOrThrow("host_name"))
                rooms.add(Room(id, label, description, code, host_id, host_name))
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return rooms
    }

    fun delete(room_id : Int) {
        base.delete("room", "id = ?", arrayOf(room_id.toString()))
    }

}