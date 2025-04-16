package com.example.partymusicapp.model

data class API_Response<T> (
    val message: String,
    val statut: String,
    val data: T?
)
