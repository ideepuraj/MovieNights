package com.movienight.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MovieRepository(baseUrl: String) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    suspend fun getMovies(): List<Movie> = api.getMovies()
}
