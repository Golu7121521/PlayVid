package com.playvid.presentation.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Makes small still-frame thumbnails for MediaStore videos, cached in memory so scrolling back
 * doesn't decode them again. Works on every API level PlayVid supports (21+).
 */
object VideoThumbnailLoader {
    private const val THUMB_WIDTH = 320
    private const val THUMB_HEIGHT = 180

    // A slice of the app's heap (never less than 4 MB): thumbnails are small, the rest stays free for playback.
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).toInt().coerceAtLeast(4 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** The thumbnail for a content:// video [uri], or null if none could be made. */
    suspend fun load(context: Context, uri: String): Bitmap? {
        cache.get(uri)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { decode(context, Uri.parse(uri)) } ?: return null
        cache.put(uri, bitmap)
        return bitmap
    }

    private fun decode(context: Context, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: the system makes (and caches) the thumbnail itself at the requested size.
            context.contentResolver.loadThumbnail(uri, Size(THUMB_WIDTH, THUMB_HEIGHT), null)
        } else {
            // Older Android: pull a frame ourselves, one second in (or the first frame for very short clips),
            // then scale it down so a 1080p frame doesn't sit in memory.
            val frame = MediaMetadataRetriever().run {
                try {
                    setDataSource(context, uri)
                    getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: getFrameAtTime(0L)
                } finally {
                    release()
                }
            }
            frame?.let { original ->
                val scaled = Bitmap.createScaledBitmap(original, THUMB_WIDTH, THUMB_HEIGHT, true)
                if (scaled !== original) original.recycle()
                scaled
            }
        }
    } catch (_: Exception) {
        null
    }
}

/** A 16:9 still frame of a local video, with a play icon while it loads or if no frame is available. */
@Composable
fun VideoThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val thumb by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = VideoThumbnailLoader.load(context, uri)?.asImageBitmap()
    }
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(placeholder),
        contentAlignment = Alignment.Center
    ) {
        val image = thumb
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
