package com.playvid.playback

import com.playvid.domain.model.PlaylistEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Parses a plain M3U/M3U8 *channel list* (one entry per `#EXTINF` + URL pair, or just a bare URL
 * per line) into playable entries. Not for HLS media/master manifests — see [looksLikeHlsManifest].
 */
object M3uParser {

    fun parse(text: String): List<PlaylistEntry> {
        val out = mutableListOf<PlaylistEntry>()
        var pendingTitle: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF") ->
                    pendingTitle = line.substringAfter(',', missingDelimiterValue = "").trim().ifBlank { null }
                line.startsWith("#") -> {} // any other tag: ignore
                else -> {
                    val title = pendingTitle ?: line.substringAfterLast('/').substringBefore('?').ifBlank { line }
                    out += PlaylistEntry(id = 0, title = title, url = line)
                    pendingTitle = null
                }
            }
        }
        return out
    }

    /** True if this is an actual HLS stream manifest (one playable video), not an IPTV channel list. */
    fun looksLikeHlsManifest(text: String): Boolean =
        text.contains("#EXT-X-STREAM-INF") || text.contains("#EXT-X-TARGETDURATION") || text.contains("#EXT-X-VERSION")

    /**
     * Adding a URL from the Playlist tab: a remote .m3u/.m3u8 *channel list* expands into many
     * entries; an actual HLS stream, or any other link, is added as a single playable entry.
     */
    suspend fun resolveUrlToEntries(client: OkHttpClient, url: String): List<PlaylistEntry> {
        val looksLikePlaylistFile = url.substringBefore('?').lowercase().let { it.endsWith(".m3u") || it.endsWith(".m3u8") }
        val fallback = listOf(PlaylistEntry(id = 0, title = titleFromUrl(url), url = url))
        if (!looksLikePlaylistFile) return fallback

        val text = try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        } catch (e: IOException) {
            null
        } ?: return fallback

        if (looksLikeHlsManifest(text)) return fallback
        val parsed = parse(text)
        return parsed.ifEmpty { fallback }
    }

    private fun titleFromUrl(url: String): String =
        url.substringBefore('?').substringAfterLast('/').ifBlank { url }
}
