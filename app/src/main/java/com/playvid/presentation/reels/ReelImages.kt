package com.playvid.presentation.reels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.playvid.PlayVidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Minimal remote-image loader (no extra library): OkHttp + BitmapFactory + small LRU cache. */
object ReelImages {
    private val cache = LruCache<String, Bitmap>(32)

    suspend fun load(client: OkHttpClient, url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        val bmp = try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val bytes = if (r.isSuccessful) r.body?.bytes() else null
                bytes?.let { decode(it) }
            }
        } catch (e: Exception) {
            null
        }
        if (bmp != null) cache.put(url, bmp)
        bmp
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

@Composable
fun rememberRemoteImage(url: String?): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, url) {
        value = if (url.isNullOrBlank()) {
            null
        } else {
            val client = (context.applicationContext as PlayVidApp).container.httpClient
            ReelImages.load(client, url)?.asImageBitmap()
        }
    }
}
