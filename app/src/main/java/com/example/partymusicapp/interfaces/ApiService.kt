package com.example.partymusicapp.interfaces

import com.example.partymusicapp.TOKEN
import com.example.partymusicapp.model.API_Response
//import com.example.partymusicapp.model.Dev
import com.example.partymusicapp.model.Music
import com.example.partymusicapp.model.Room
import com.example.partymusicapp.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface ApiService {
//    @Headers("Authorization: Bearer $TOKEN")
//    @POST("ping")
//    suspend fun getPing(): Response<Dev>

    // USER
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

    // ROOM
    data class CreateRoomRequest(
        val id : Int,
        val label: String,
        val description: String
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("room/create")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<API_Response<Room>>

    data class GetUserRoomRequest(
        val id : Int
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("room/get")
    suspend fun getUserRoom(@Body request: GetUserRoomRequest): Response<API_Response<ArrayList<Room>>>

    data class JoinRoomRequest(
        val user_id : Int,
        val code : String
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("room/join")
    suspend fun joinRoom(@Body request: JoinRoomRequest) : Response<API_Response<Room>>

    // MUSIC
    data class GetMusicRequest(
        val room_id : Int
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("music/get")
    suspend fun getMusic(@Body request: GetMusicRequest) : Response<API_Response<ArrayList<Music>>>

//    data class AllMusicRequest(
//        val user_id : Int
//    )
//    @Headers("Authorization: Bearer $TOKEN")
//    @POST("music/get")
//    suspend fun allMusic(@Body request: AllMusicRequest) : Response<API_Response<ArrayList<Music>>>

    data class AddMusicRequest(
        val room_id : Int,
        val user_id : Int,
        val title : String,
        val artist : String,
        val link : String
    )
    @Headers("Authorization: Bearer $TOKEN")
    @POST("music/create")
    suspend fun addMusic(@Body request: AddMusicRequest) : Response<API_Response<Music>>
}