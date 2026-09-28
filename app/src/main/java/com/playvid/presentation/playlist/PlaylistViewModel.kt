package com.playvid.presentation.playlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playvid.domain.model.PlaylistEntry
import com.playvid.domain.repository.PlaylistRepository
import com.playvid.playback.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class PlaylistViewModel(
    private val repo: PlaylistRepository,
    private val httpClient: OkHttpClient
) : ViewModel() {

    val entries: StateFlow<List<PlaylistEntry>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onUrlInputChange(value: String) { _urlInput.value = value }

    fun dismissMessage() { _message.value = null }

    /** "Add URL": a remote .m3u/.m3u8 channel list expands into many entries; anything else adds as one. */
    fun addUrl() {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            val entries = try {
                withContext(Dispatchers.IO) { M3uParser.resolveUrlToEntries(httpClient, url) }
            } catch (e: Exception) {
                emptyList()
            }
            if (entries.isEmpty()) {
                _message.value = "Couldn't add that link."
            } else {
                repo.addAll(entries)
                _urlInput.value = ""
                _message.value = if (entries.size > 1) "Added ${entries.size} items." else "Added."
            }
            _busy.value = false
        }
    }

    /** "Pick": a local .m3u/.m3u8 file, expanded into its channel entries. */
    fun addFromPickedFile(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _busy.value = true
            val text = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            } catch (e: Exception) {
                null
            }
            val entries = text?.let { M3uParser.parse(it) }.orEmpty()
            if (entries.isEmpty()) {
                _message.value = "Couldn't find any channels in that file."
            } else {
                repo.addAll(entries)
                _message.value = "Added ${entries.size} items."
            }
            _busy.value = false
        }
    }

    fun remove(id: Long) = viewModelScope.launch { repo.delete(id) }
}
