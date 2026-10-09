package com.playvid.presentation.server

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playvid.PlayVidApp
import com.playvid.domain.model.PlaybackRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    onPlay: (PlaybackRequest) -> Unit,
    viewModel: ServerViewModel = viewModel()
) {
    val links by viewModel.links.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingUrl by viewModel.playingUrl.collectAsState()

    val context = LocalContext.current
    // Validating logic directly from the App Container (Supports all links)
    val validateUrl = remember { (context.applicationContext as PlayVidApp).container.validateUrl }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Links") },
                actions = {
                    IconButton(onClick = { viewModel.fetchLinks() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Links")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (links.isEmpty()) {
                Text(
                    text = "No link available",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(links) { link ->
                        val isThisVideoLoading = playingUrl == link.url.trim()
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable(enabled = !isThisVideoLoading && playingUrl == null) {
                                    // Click hone par ViewModel ka playLink chalega
                                    viewModel.playLink(
                                        link = link,
                                        validateUrl = validateUrl,
                                        onValid = onPlay,
                                        onInvalid = { errorMsg ->
                                            Toast.makeText(context, "Invalid link: $errorMsg", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = link.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Jab video process ho raha hoga, card pe loader dikhega
                                if (isThisVideoLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
