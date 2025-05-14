package com.example.partymusicapp.model

data class YouTubeSearchResponse(
    val items: List<YouTubeVideoItem>
)

data class YouTubeVideoItem(
    val id: VideoId,
    val snippet: Snippet
)

data class VideoId(
    val videoId: String
)

data class Snippet(
    val title: String,
    val channelTitle: String, // Nom de l'artiste
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val default: Thumbnail
)

data class Thumbnail(
    val url: String
)
