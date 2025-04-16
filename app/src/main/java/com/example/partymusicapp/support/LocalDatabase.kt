package com.example.partymusicapp.support

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalDatabase (context: Context) : SQLiteOpenHelper(context, "user", null, 1){
    override fun onCreate(db: SQLiteDatabase?) {
        val sql = """
              CREATE TABLE user (
              `id` INTEGER PRIMARY KEY, 
              `name` TEXT NOT NULL, 
              `email` TEXT UNIQUE NOT NULL, 
              `password` TEXT, 
              `token` TEXT)""".trimIndent()
        db?.execSQL(sql)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS jeu")
        onCreate(db)
    }

}