package com.playvid.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException // <- Yeh import zaroori hai
import java.net.URLEncoder

class InstagramResolver(private val httpClient: OkHttpClient) {

    suspend fun resolve(instagramUrl: String): String = withContext(Dispatchers.IO) {
        // Yahan apna backend URL daalein jahan aapne Flask app deploy kiya hai
        val backendBaseUrl = "https://YOUR_BACKEND_URL_HERE/download"
        val encodedUrl = URLEncoder.encode(instagramUrl, "UTF-8")
        val targetUrl = "$backendBaseUrl?url=$encodedUrl"

        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to resolve Instagram link: HTTP ${response.code}")
            }
            response.request.url.toString()
        }
    }

    companion object {
        fun isInstagramUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains("instagram.com/reel/") ||
                   lower.contains("instagram.com/reels/") ||
                   lower.contains("instagram.com/p/") ||
                   lower.contains("instagram.com/tv/")
        }
    }
}
