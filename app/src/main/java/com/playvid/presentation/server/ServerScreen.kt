package com.playvid.presentation.server

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playvid.PlayVidApp
import com.playvid.domain.model.PlaybackRequest
import com.playvid.domain.usecase.UrlValidation
import kotlinx.coroutines.launch

@Composable
fun ServerScreen(
    onPlay: (PlaybackRequest) -> Unit,
    viewModel: ServerViewModel = viewModel()
) {
    val links by viewModel.links.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Context aur usecase extract karein taaki hum URL ko resolve kar sakein
    val context = LocalContext.current
    val validateUrl = remember { (context.applicationContext as PlayVidApp).container.validateUrl }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Server Links") })
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // Click hone par pehle URL ko validate aur process karein
                                    scope.launch {
                                        when (val result = validateUrl(link.url)) {
                                            is UrlValidation.Valid -> {
                                                onPlay(
                                                    PlaybackRequest(
                                                        source = result.url, // Processed/Resolved link
                                                        title = link.title,  // Backend se aaya naam
                                                        isLocal = false
                                                    )
                                                )
                                            }
                                            is UrlValidation.Invalid -> {
                                                Toast.makeText(context, "Invalid link: ${result.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = link.title,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
