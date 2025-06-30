package com.example.partymusicapp.model

data class Music (
    val id: Int,
    var title: String,
    var artist: String,
    var duration: String,
    var link: String,
    var likes: Int,
    var playable: Int,
    var user_id: Int,
    var room_id: Int,
    var playlists: MutableList<Playlist> = mutableListOf(),
    var user_name: String,
) {
    override fun toString(): String {
        return "Music -> id : '$id', title : '$title', artist : '$artist', duration : '$duration', likes : '${likes}', link : '$link', playable : '${playable}', room_id : '${room_id}', user_id : '${user_id}', user_name : '$user_name'."
    }
}
