package com.example.partymusicapp.support

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.partymusicapp.model.User

class UserDAO {
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
        base.delete("user", null, null)
    }

    public fun insert(user: User?) : Boolean {
        open()
        empty() // Vider la table avant d'insérer un nouvel utilisateur pour ne garder que celui qui est connécté
        val values = ContentValues()
        values.put("id", user?.id)
        values.put("name", user?.name)
        values.put("email", user?.email)
        values.put("password", user?.password)
        values.put("token", user?.token)
        val result = base.insert("user", null, values).toInt() // enregistrer l'utilisateur dans la base de données
        close()
        return result != -1
    }

    public fun get() : User? {
        open()
        val cursor = base.query("user", null, null, null, null, null, null) // Récupérer le seul utilisateur connecté
        if (cursor.count == 0 || !cursor.moveToFirst()) {
            cursor.close()
            close()
            return null
        }
        cursor.moveToFirst()
        // Récupérer les colones de l'utilisateur
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
        val password = cursor.getString(cursor.getColumnIndexOrThrow("password"))
        val token = cursor.getString(cursor.getColumnIndexOrThrow("token"))

        cursor.close()
        close()

        // Vérification stricte
        if (name.isNullOrBlank() || email.isNullOrBlank() || password.isNullOrBlank() || token.isNullOrBlank()) {
            return null
        }

        return User(id, name, email, password, token)
    }
}