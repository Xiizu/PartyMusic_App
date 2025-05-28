package com.example.partymusicapp.support

import com.example.partymusicapp.BASE_URL
import com.example.partymusicapp.interfaces.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Database {
    // Api Request
    object RetrofitClient {
        val instance: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}