package com.example.partymusicapp.interfaces

import com.example.partymusicapp.TOKEN
import com.example.partymusicapp.YT_API_KEY
import com.example.partymusicapp.model.API_Response
import com.example.partymusicapp.model.Dev
import com.example.partymusicapp.model.Music
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.model.User
import com.example.partymusicapp.model.YouTubeSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.Continuation


interface ApiServiceYT {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,               // → mots-clés
        @Query("type") type: String = "video",
        @Query("key") apiKey: String = YT_API_KEY,            // → clé API
        @Query("maxResults") maxResults: Int = 10
    ): Response<YouTubeSearchResponse>

    @GET("search")
    suspend fun initApiRequest(
        @Query("part") part: String = "snippet",
        @Query("q") query: String = "made by Xiizu",               // → mots-clés
        @Query("type") type: String = "video",
        @Query("key") apiKey: String = YT_API_KEY,            // → clé API
        @Query("maxResults") maxResults: Int = 1
    )
}