package com.example.partymusicapp.support

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalDatabase (context: Context) : SQLiteOpenHelper(context, "user", null, 7){
    override fun onCreate(db: SQLiteDatabase?) {
        val sql_user = """
              CREATE TABLE user (
              `id` INTEGER PRIMARY KEY, 
              `name` TEXT NOT NULL, 
              `email` TEXT UNIQUE NOT NULL, 
              `password` TEXT, 
              `token` TEXT)""".trimIndent()
        db?.execSQL(sql_user)
        val sql_room = """
              CREATE TABLE room (
              `id` INTEGER PRIMARY KEY, 
              `label` TEXT NOT NULL, 
              `description` TEXT, 
              `code` TEXT UNIQUE NOT NULL, 
              `host_id` INTEGER NOT NULL,
              `host_name` TEXT NOT NULL)""".trimIndent()
        db?.execSQL(sql_room)
        val sql_music = """
            CREATE TABLE music (
            `id` INTEGER PRIMARY KEY, 
            `title` TEXT NOT NULL, 
            `artist` TEXT NOT NULL, 
            `link` TEXT NOT NULL, 
            `duration` TEXT NOT NULL, 
            `likes` INTEGER NOT NULL, 
            `playable` INTEGER NOT NULL, 
            `user_name` TEXT NOT NULL, 
            `user_id` INTEGER NOT NULL, 
            `room_id` INTEGER NOT NULL)""".trimIndent()
        db?.execSQL(sql_music)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS user")
        db?.execSQL("DROP TABLE IF EXISTS room")
        db?.execSQL("DROP TABLE IF EXISTS music")
        db?.execSQL("DROP TABLE IF EXISTS playlist")
        onCreate(db)
    }

}