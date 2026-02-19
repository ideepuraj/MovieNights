package com.movienight.data

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: String,
    val title: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("stream_url") val streamUrl: String
)
