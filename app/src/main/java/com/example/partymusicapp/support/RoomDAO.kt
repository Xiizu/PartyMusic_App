package com.example.partymusicapp.support

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.model.User

class RoomDAO {
    lateinit var base: SQLiteDatabase
    lateinit var localDatabase: LocalDatabase

    public fun init(context: Context) {
        localDatabase = LocalDatabase(context)
    }

    public fun open() {
        base = localDatabase.writableDatabase
    }

    public fun close() {
        base.close()
    }

    public fun empty() {
        base.delete("room", null, null)
    }

    public fun insert(room: Room?) : Boolean {
        open()
        val values = ContentValues()
        values.put("id", room?.id)
        values.put("label", room?.label)
        values.put("description", room?.description)
        values.put("code", room?.code)
        values.put("host_id", room?.host_id)
        val result = base.insert("room", null, values).toInt() // enregistrer la room dans la base de données
        close()
        return result != -1
    }

    public fun get(room_id : Int) : Room? {
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
                    cursor.close()
                    close()
                    return Room(id, label, description, code, host_id)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return null
    }

    public fun index() : ArrayList<Room> {
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
                rooms.add(Room(id, label, description, code, host_id))
            } while (cursor.moveToNext())
        }
        cursor.close()
        close()
        return rooms
    }

    public fun delete(room_id : Int) {
        base.delete("room", "id = ?", arrayOf(room_id.toString()))
    }

}