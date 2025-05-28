package com.example.partymusicapp.interfaces

import com.example.partymusicapp.YT_API_KEY
import com.example.partymusicapp.model.YouTubeSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


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