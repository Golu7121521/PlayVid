package com.playvid.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class InstagramResolver(private val httpClient: OkHttpClient) {

    suspend fun resolve(instagramUrl: String): String = withContext(Dispatchers.IO) {
        // Yahan apna backend URL daalein (Jaise Render/Railway ya local IP ka URL)
        val backendUrl = "https://your-backend-service.onrender.com/download?url=${java.net.URLEncoder.encode(instagramUrl, "UTF-8")}"
        
        val request = Request.Builder()
            .url(backendUrl)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to resolve Instagram link: ${response.code}")
            }
            val bodyString = response.body?.string() ?: throw IOException("Empty response from backend")
            
            // Agar backend direct file stream bhej raha hai ya JSON bhej raha hai, 
            // aapke Flask backend ke anusaar agar direct video stream bhej raha hai toh response.request.url.toString() use kar sakte hain
            // Ya agar koi redirect URL milta hai:
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
