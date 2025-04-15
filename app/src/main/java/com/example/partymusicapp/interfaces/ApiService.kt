package com.example.partymusicapp.interfaces

import com.example.partymusicapp.model.Account
import retrofit2.Response
import com.example.partymusicapp.TOKEN
import com.example.partymusicapp.model.Dev
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Authorization: Bearer $TOKEN")
    @POST("ping")
    suspend fun getPing(): Response<Dev>

    @POST("accounts")
    suspend fun getAccounts(): Response<List<Account>>

}