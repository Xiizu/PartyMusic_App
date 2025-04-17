package com.example.partymusicapp.interfaces

import com.example.partymusicapp.model.User
import retrofit2.Response
import com.example.partymusicapp.TOKEN
import com.example.partymusicapp.model.API_Response
import com.example.partymusicapp.model.Dev
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Authorization: Bearer $TOKEN")
    @POST("ping")
    suspend fun getPing(): Response<Dev>

    data class LoginRequest(
        val email: String,
        val password: String
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("user/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<API_Response<User>>

    data class RegisterRequest(
        val name: String,
        val email: String,
        val password: String
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("user/create")
    suspend fun registerUser(@Body request: RegisterRequest): Response<API_Response<User>>

}