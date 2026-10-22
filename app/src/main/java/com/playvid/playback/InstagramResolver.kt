package com.playvid.playback

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Turns an Instagram reel link into a playable .mp4 link using your own backend (Render).
 * The backend downloads + merges video/audio and returns `play_url` from /extract.
 * Blocking: call from a background thread.
 */
class InstagramResolver(httpClient: OkHttpClient) {

    // Render free tier can take 30-60s to wake up, then 10-30s to fetch + merge the reel,
    // so this client needs much longer timeouts than the shared streaming client.
    private val client: OkHttpClient = httpClient.newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .callTimeout(200, TimeUnit.SECONDS)
        .build()

    fun resolve(rawUrl: String): String {
        val reelUrl = cleanUrl(rawUrl) ?: throw IOException("Invalid Instagram link")

        val endpoint = "$BACKEND_URL/extract".toHttpUrl().newBuilder()
            .addQueryParameter("url", reelUrl)
            .apply { if (API_KEY.isNotBlank()) addQueryParameter("key", API_KEY) }
            .build()

        client.newCall(Request.Builder().url(endpoint).build()).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val json = try { JSONObject(body) } catch (_: Exception) { null }
            if (!resp.isSuccessful || json == null) {
                val detail = json?.optString("detail").orEmpty().ifBlank { json?.optString("error").orEmpty() }
                throw IOException("Backend error ${resp.code} $detail".trim())
            }
            val playUrl = json.optString("play_url")
            if (playUrl.isBlank()) throw IOException("Backend did not return play_url")
            return playUrl
        }
    }

    companion object {
        /** Your Render deployment (no trailing slash). */
        const val BACKEND_URL = "https://insta-api-6dps.onrender.com"
        /** Only needed if you set API_KEY on the server; leave empty otherwise. */
        const val API_KEY = ""

        private val REEL_PATH = Regex("^/(reel|reels|p|tv)/([\\w-]+)")

        fun isInstagramUrl(url: String): Boolean {
            val host = try { URI(url.trim()).host?.lowercase() } catch (_: Exception) { null } ?: return false
            val ok = host == "instagram.com" || host.endsWith(".instagram.com") || host == "instagr.am"
            return ok && cleanUrl(url) != null
        }

        /** Drops tracking params (?cplk=..., ?igsh=...) and keeps only https://www.instagram.com/<type>/<code>/ */
        fun cleanUrl(url: String): String? {
            val uri = try { URI(url.trim()) } catch (_: Exception) { return null }
            val m = REEL_PATH.find(uri.path.orEmpty()) ?: return null
            return "https://www.instagram.com/${m.groupValues[1]}/${m.groupValues[2]}/"
        }
    }
}
