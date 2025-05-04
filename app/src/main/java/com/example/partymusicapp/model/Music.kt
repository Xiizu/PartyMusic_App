package com.example.partymusicapp.model

data class Music (
    val id: Int,
    var title: String,
    var artist: String,
    var duration: String,
    var likes: Int,
    var link: String,
    var playable: Boolean,
    var room_id: Int,
    var user_id: Int,
    var user_name: String)

{
    override fun toString(): String {
        return "Music -> id : '$id', title : '$title', artist : '$artist', duration : '$duration', likes : '${likes.toString()}', link : '$link', playable : '${playable.toString()}', room_id : '${room_id.toString()}', user_id : '${user_id.toString()}', user_name : '$user_name"
    }
}