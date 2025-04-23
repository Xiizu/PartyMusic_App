package com.example.partymusicapp.support

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalDatabase (context: Context) : SQLiteOpenHelper(context, "user", null, 2){
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
              `host_id` INTEGER NOT NULL)""".trimIndent()
        db?.execSQL(sql_room)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS user")
        db?.execSQL("DROP TABLE IF EXISTS room")
        onCreate(db)
    }

}