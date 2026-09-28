package com.playvid.presentation.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playvid.domain.model.PlaybackRequest
import com.playvid.presentation.AppViewModelProvider
import com.playvid.presentation.components.EmptyState
import com.playvid.presentation.components.LocalNotifier
import com.playvid.presentation.components.VideoRow

/**
 * Playlist: "Pick" a local .m3u/.m3u8 file, or paste a URL (a remote channel-list link expands
 * into many entries; anything else is added as one). Tap an entry to play it.
 */
@Composable
fun PlaylistScreen(
    onPlay: (PlaybackRequest) -> Unit,
    vm: PlaylistViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val notifier = LocalNotifier.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val urlInput by vm.urlInput.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let { notifier.show(it); vm.dismissMessage() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.addFromPickedFile(context, uri)
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)) {
        item {
            Text("Playlist", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = vm::onUrlInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(".m3u / .m3u8 or video URL") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = vm::addUrl, enabled = !busy && urlInput.isNotBlank()) { Text("Add") }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pick .m3u File") }
            if (busy) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.height(20.dp).width(20.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (entries.isEmpty() && !busy) {
            item { EmptyState("No playlist yet", "Pick an .m3u file or add a URL above.") }
        } else {
            items(entries, key = { it.id }) { entry ->
                VideoRow(
                    title = entry.title,
                    details = entry.url,
                    progress = 0f,
                    onClick = { onPlay(entry.toRequest()) },
                    onRemove = { vm.remove(entry.id) }
                )
            }
        }
    }
}
