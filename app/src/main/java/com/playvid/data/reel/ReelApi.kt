package com.playvid.data.reel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * RapidAPI credentials for the Reels tab.
 * NOTE: keep your GitHub repo PRIVATE (the key ends up inside the APK anyway),
 * or move the key to local.properties / BuildConfig later.
 */
object ReelConfig {
    const val RAPID_API_HOST = "social-media-video-downloader.p.rapidapi.com"
    const val RAPID_API_KEY = "824dddee65msh76dbe040b14bfe4p12ab6ajsnc6532155d4f8"
}

data class ReelLink(val quality: String, val link: String, val type: String) {
    val isAudio: Boolean get() = quality.uppercase().contains("AUDIO") || type == "audio"
}

data class ReelMedia(val links: List<ReelLink>, val picture: String?)

/** Same extraction call the web version (i_s.html) makes. */
class ReelApi(private val client: OkHttpClient) {

    suspend fun fetch(cleanUrl: String): ReelMedia = withContext(Dispatchers.IO) {
        val target = "https://${ReelConfig.RAPID_API_HOST}/smvd/get/all" +
            "?url=${URLEncoder.encode(cleanUrl, "UTF-8")}&filename=InstaSave_Media"
        val request = Request.Builder()
            .url(target)
            .get()
            .header("X-RapidAPI-Key", ReelConfig.RAPID_API_KEY)
            .header("X-RapidAPI-Host", ReelConfig.RAPID_API_HOST)
            .build()

        client.newCall(request).execute().use { response ->
            val json = JSONObject(response.body?.string().orEmpty())
            val arr = json.optJSONArray("links") ?: throw IOException("No payload structural tracks found.")
            val links = ArrayList<ReelLink>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val link = o.optString("link")
                if (link.isBlank() || link == "null") continue
                links.add(ReelLink(o.optString("quality").let { if (it == "null") "" else it }, link, o.optString("type")))
            }
            if (links.isEmpty()) throw IOException("No payload structural tracks found.")
            ReelMedia(links, json.optString("picture").takeIf { it.isNotBlank() && it != "null" })
        }
    }
}
