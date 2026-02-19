package com.movienight.data

import retrofit2.http.GET

interface ApiService {
    @GET("api/movies")
    suspend fun getMovies(): List<Movie>
}
