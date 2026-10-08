package com.playvid.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    init {
        fetchLinks()
    }

    fun fetchLinks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Apna Render API URL yahan dalein
                val apiUrl = "https://playvid-server.onrender.com/"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val parsedLinks = mutableListOf<ServerLink>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        parsedLinks.add(
                            ServerLink(title = obj.getString("title"), url = obj.getString("url"))
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
}
