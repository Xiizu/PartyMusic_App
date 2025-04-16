package com.example.partymusicapp.model;

data class User (
    val id: Int,
    var name: String,
    var email: String,
    var password: String,
    var token: String)
{
    override fun toString(): String {
        return "User -> id : '$id', name : '$name', email : '$email', password : '$password', token : '$token"
    }
}
