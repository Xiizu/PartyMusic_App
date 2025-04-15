package com.example.partymusicapp.model;

data class Account (
    var username: String,
    var email: String,
    var password: String)
{
    override fun toString(): String {
        return "Account -> username : '$username', email : '$email', password : '$password'"
    }
}
