package com.movienight.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class MovieCategory(val slug: String, val path: String, val displayName: String) {
    MALAYALAM("malayalam", "/category/malayalam-featured", "Malayalam"),
    TAMIL("tamil", "/category/tamil-featured", "Tamil"),
    HINDI("hindi", "/category/bollywood-featured", "Hindi"),
    HOLLYWOOD("hollywood", "/category/hollywood-featured", "Hollywood"),
}

class MovieRulzScraper(private val context: Context, private val baseUrl: String) {

    companion object {
        private const val CACHE_EXPIRY_MS = 24L * 60 * 60 * 1000  // 24 hours
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "movierulz").also { it.mkdirs() }

    suspend fun getMovies(category: MovieCategory, page: Int): List<Movie> =
        withContext(Dispatchers.IO) {
            getCachedMovies(category, page)
        }

    private fun getCachedMovies(category: MovieCategory, page: Int): List<Movie> {
        val cacheFile = File(cacheDir, "movies_${category.slug}_p$page.json")
        if (cacheFile.exists()) {
            val age = System.currentTimeMillis() - cacheFile.lastModified()
            if (age < CACHE_EXPIRY_MS) {
                runCatching { return parseCache(cacheFile.readText()) }
            }
        }
        return scrapeFromWeb(category, page).also { movies ->
            if (movies.isNotEmpty()) saveCache(cacheFile, movies)
        }
    }

    private fun scrapeFromWeb(category: MovieCategory, page: Int): List<Movie> {
        val url = if (page == 1) "$baseUrl${category.path}"
                  else "$baseUrl${category.path}/page/$page"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val html = client.newCall(request).execute().use { resp ->
                resp.body?.string() ?: return emptyList()
            }
            val doc = Jsoup.parse(html)
            doc.select("div.content.home_style ul li").mapNotNull { item ->
                val link = item.selectFirst("a") ?: return@mapNotNull null
                val img = item.selectFirst("img")
                Movie(
                    title = (link.attr("title").ifBlank { "Untitled" })
                        .split("(").first().trim(),
                    url = link.attr("href"),
                    thumbnail = img?.attr("src") ?: "",
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseCache(json: String): List<Movie> {
        val arr = JSONObject(json).getJSONArray("movies")
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            Movie(
                title = obj.getString("title"),
                url = obj.getString("url"),
                thumbnail = obj.getString("thumbnail"),
            )
        }
    }

    private fun saveCache(file: File, movies: List<Movie>) {
        val arr = JSONArray()
        movies.forEach { m ->
            arr.put(JSONObject().apply {
                put("title", m.title)
                put("url", m.url)
                put("thumbnail", m.thumbnail)
            })
        }
        file.writeText(JSONObject().apply { put("movies", arr) }.toString())
    }
}
