package com.playvid.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playvid.domain.model.PlaybackRequest
import com.playvid.domain.usecase.UrlValidation
import com.playvid.domain.usecase.ValidateStreamUrlUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class ServerLink(val title: String, val url: String)

class ServerViewModel : ViewModel() {
    private val _links = MutableStateFlow<List<ServerLink>>(emptyList())
    val links = _links.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Ek time par ek hi video load hone ke liye (prevents double-click buffering issue)
    private val _playingUrl = MutableStateFlow<String?>(null)
    val playingUrl = _playingUrl.asStateFlow()

    init {
        fetchLinks()
    }

    fun fetchLinks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Apna original Render API URL yahan dalein
                val apiUrl = "https://playvid-server.onrender.com/api/links"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val parsedLinks = mutableListOf<ServerLink>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        parsedLinks.add(
                            ServerLink(
                                title = obj.getString("title").trim(),
                                // Trim lagana bohot zaroori hai JSON URL errors se bachne ke liye
                                url = obj.getString("url").trim()
                            )
                        )
                    }
                    _links.value = parsedLinks
                } else {
                    _links.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _links.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 
     * Bilkul HomeViewModel ke play() function ki tarah kaam karta hai. 
     * Har type ke link (M3U8, MP4, etc.) ko support karega.
     */
    fun playLink(
        link: ServerLink,
        validateUrl: ValidateStreamUrlUseCase,
        onValid: (PlaybackRequest) -> Unit,
        onInvalid: (String) -> Unit
    ) {
        if (_playingUrl.value != null) return // Agar already load ho raha hai toh ignore karo
        
        val cleanUrl = link.url.trim()
        
        when (val result = validateUrl(cleanUrl)) {
            is UrlValidation.Invalid -> {
                onInvalid(result.message)
            }
            is UrlValidation.Valid -> viewModelScope.launch {
                _playingUrl.value = cleanUrl
                
                // result.url mein properly validated URL hoti hai (exactly like Home tab)
                onValid(
                    PlaybackRequest(
                        source = result.url,
                        title = link.title.ifBlank { result.title },
                        isLocal = false
                    )
                )
                
                // Loading screen ko 600ms tak hold karte hain jab tak PlayerScreen open na ho jaye
                delay(600)
                _playingUrl.value = null
            }
        }
    }
}
