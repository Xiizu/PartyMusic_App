package com.example.partymusicapp.support

import com.example.partymusicapp.YT_API_URL
import com.example.partymusicapp.interfaces.ApiServiceYT
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class YouTubeAPI {
    // Api Request
    object RetrofitClientYT {
        val instanceYT: ApiServiceYT by lazy {
            Retrofit.Builder()
                .baseUrl(YT_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiServiceYT::class.java)
        }
    }
}