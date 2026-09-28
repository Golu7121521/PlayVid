package com.playvid.domain.model

/** One entry in the watch history. [source] is the URL or the content:// URI string. */
data class HistoryItem(
    val id: Long,
    val title: String,
    val source: String,
    val isLocal: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long
) {
    /** 0f..1f watch progress. */
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    fun toRequest() = PlaybackRequest(source = source, title = title, isLocal = isLocal)
}

/** One entry in a user-built playlist (from a picked .m3u/.m3u8 file or a pasted URL). */
data class PlaylistEntry(val id: Long, val title: String, val url: String) {
    fun toRequest() = PlaybackRequest(source = url, title = title, isLocal = false)
}

/** Everything the player needs to start playing something. */
data class PlaybackRequest(
    val source: String,
    val title: String,
    val isLocal: Boolean
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User preferences. System theme is the default. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val resumePlayback: Boolean = true,
    /** Swipe up/down on the left half of the player. */
    val brightnessGesture: Boolean = true,
    /** Swipe up/down on the right half of the player. */
    val volumeGesture: Boolean = true,
    /** Starting quality for a video opened from a link. 0 = Auto (highest available). */
    val preferredQuality: Int = 0,
    /** When on, always starts at the lowest available quality to save mobile data. */
    val dataSaver: Boolean = false,
    /** Press and hold anywhere on the video to play at [holdSpeedMultiplier], release to go back. */
    val holdSpeedGesture: Boolean = true,
    /** 2..5x, used by [holdSpeedGesture]. */
    val holdSpeedMultiplier: Int = 2,
    /** Auto-enter Picture-in-Picture when leaving the app while a video is playing. The PiP button
     *  in the player controls always works regardless of this setting. */
    val pipEnabled: Boolean = true,
    /** Keep playing (audio/video, in the mini player) after pressing back out of the full player. */
    val backgroundPlayback: Boolean = true
)
