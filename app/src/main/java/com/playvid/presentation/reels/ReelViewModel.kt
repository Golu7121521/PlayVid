package com.playvid.presentation.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playvid.PlayVidApp
import com.playvid.data.reel.ReelApi
import com.playvid.data.reel.ReelHistoryItem
import com.playvid.data.reel.ReelLink
import com.playvid.data.reel.ReelStore
import com.playvid.download.DownloadJob
import com.playvid.download.DownloadKind
import com.playvid.download.DownloadRepository
import com.playvid.download.DownloadService
import com.playvid.download.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class ReelResultUi(
    val links: List<ReelLink>,
    val thumb: String?,
    val previewUrl: String,
    val originalUrl: String
) {
    val hasAudio: Boolean get() = links.any { it.isAudio }
}

data class ReelUi(
    val input: String = "",
    val loading: Boolean = false,
    val progress: Int = 0,
    val status: String = "Parsing Link...",
    val result: ReelResultUi? = null,
    val history: List<ReelHistoryItem> = emptyList(),
    /** button key -> DownloadRepository job id (used to show "Downloading..." on that button) */
    val downloads: Map<String, Int> = emptyMap(),
    val dark: Boolean = true
)

class ReelViewModel(private val app: PlayVidApp) : ViewModel() {

    private val store = ReelStore(app)
    private val api = ReelApi(app.container.httpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build())

    private val _ui = MutableStateFlow(ReelUi(history = store.history(), dark = store.dark))
    val ui: StateFlow<ReelUi> = _ui.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var job: Job? = null

    fun message(text: String) { _messages.tryEmit(text) }

    fun onInput(text: String) = _ui.update { it.copy(input = text) }

    fun onPaste(text: String) {
        _ui.update { it.copy(input = text) }
        message("Link pasted!")
    }

    fun clear() {
        job?.cancel()
        _ui.update { it.copy(input = "", result = null, loading = false) }
        message("Link cleared!")
    }

    fun toggleTheme() {
        val next = !_ui.value.dark
        store.dark = next
        _ui.update { it.copy(dark = next) }
    }

    fun extract() {
        val input = _ui.value.input.trim()
        if (!input.contains("instagram.com")) {
            message("Invalid Instagram Link")
            return
        }
        val clean = cleanUrl(input)
        job?.cancel()
        _ui.update { it.copy(loading = true, result = null, progress = 0, status = "Connecting to Instagram Stream...") }

        job = viewModelScope.launch {
            val ticker = launch {
                var p = 0
                while (isActive) {
                    delay(250)
                    p = minOf(92, p + Random.nextInt(2, 14))
                    _ui.update {
                        it.copy(progress = p, status = if (p > 45) "Extracting HD media tracks..." else it.status)
                    }
                }
            }
            try {
                val media = api.fetch(clean)
                ticker.cancel()
                _ui.update { it.copy(progress = 100) }
                delay(400)
                app.container.playback.pause() // don't talk over the main player while previewing
                val preview = (media.links.firstOrNull { !it.quality.lowercase().contains("audio") } ?: media.links.first()).link
                _ui.update {
                    it.copy(
                        loading = false,
                        result = ReelResultUi(media.links, media.picture, preview, clean)
                    )
                }
                message("Media Engine Ready!")
                saveToHistory(media.picture, clean)
            } catch (c: CancellationException) {
                ticker.cancel()
                throw c
            } catch (e: Exception) {
                ticker.cancel()
                _ui.update { it.copy(loading = false) }
                message("Private video or server timeout.")
            }
        }
    }

    /** Video/audio download goes through the app's own DownloadService (notification + Downloads tab). */
    fun download(link: String, audio: Boolean) {
        val key = buttonKey(link, audio)
        val kind = if (audio) DownloadKind.AUDIO else DownloadKind.VIDEO
        val code = Regex("/(?:reel|reels|p|tv)/([\\w-]+)").find(_ui.value.result?.originalUrl.orEmpty())
            ?.groupValues?.get(1) ?: System.currentTimeMillis().toString()
        val title = (if (audio) "InstaSave_Audio_" else "InstaSave_Video_") + code
        val id = DownloadRepository.nextId()
        DownloadRepository.upsert(DownloadJob(id, title, kind, DownloadStatus.QUEUED, 0))
        DownloadService.enqueue(app, id, title, kind, link, null)
        _ui.update { it.copy(downloads = it.downloads + (key to id)) }
        message("Download started, please wait...")
    }

    fun openHistory(item: ReelHistoryItem) {
        _ui.update { it.copy(input = item.url) }
        extract()
    }

    fun removeHistory(index: Int) {
        val list = _ui.value.history.toMutableList()
        if (index in list.indices) list.removeAt(index)
        store.saveHistory(list)
        _ui.update { it.copy(history = list) }
        message("Removed from history!")
    }

    fun clearHistory() {
        store.saveHistory(emptyList())
        _ui.update { it.copy(history = emptyList()) }
        message("History Cleared!")
    }

    private fun saveToHistory(thumb: String?, url: String) {
        val list = _ui.value.history.toMutableList()
        if (list.none { it.url == url }) {
            list.add(0, ReelHistoryItem(thumb.orEmpty(), url))
            while (list.size > MAX_HISTORY) list.removeAt(list.lastIndex)
            store.saveHistory(list)
            _ui.update { it.copy(history = list) }
        }
    }

    private fun cleanUrl(url: String): String = try {
        val u = URI(url)
        if (u.host.isNullOrBlank()) url else "${u.scheme}://${u.host}${u.path}"
    } catch (e: Exception) {
        url
    }

    companion object {
        private const val MAX_HISTORY = 4
        fun buttonKey(link: String, audio: Boolean) = (if (audio) "a:" else "v:") + link
    }
}
