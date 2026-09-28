package com.playvid.presentation.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playvid.domain.model.HistoryItem
import com.playvid.domain.model.PlaybackRequest
import com.playvid.presentation.AppViewModelProvider
import com.playvid.presentation.components.EmptyState
import com.playvid.presentation.components.LoadingIndicator
import com.playvid.presentation.components.VideoRow
import com.playvid.presentation.util.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Files: title + search icon, "Choose Video" (system picker), list of videos opened before.
 * The system picker handles folder browsing, so no custom file manager is needed.
 */
@Composable
fun FilesScreen(
    onPlay: (PlaybackRequest) -> Unit,
    vm: FilesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val filesState by vm.files.collectAsStateWithLifecycle()
    val files = filesState

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val shown = if (files != null && query.isNotBlank()) {
        files.filter { it.title.contains(query.trim(), ignoreCase = true) }
    } else files

    // Storage Access Framework: no storage permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                // Keep access so files in the list can be reopened after an app restart.
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Provider doesn't offer persistable access; playing right now still works.
            }
            onPlay(PlaybackRequest(source = uri.toString(), title = displayName(context, uri), isLocal = true))
        }
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Files", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) query = ""
                }) {
                    Icon(
                        imageVector = if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searching) "Close search" else "Search files"
                    )
                }
            }
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("Search files") },
                    singleLine = true
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { picker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Choose Video") }
            Spacer(Modifier.height(16.dp))
        }

        when {
            shown == null -> item { LoadingIndicator() }
            files.isNullOrEmpty() -> item {
                EmptyState("No videos selected", "Choose a video from your device.")
            }
            shown.isEmpty() -> item {
                EmptyState("No results", "Try a different name.")
            }
            else -> items(shown, key = { it.id }) { file ->
                FileRow(file = file, onClick = { onPlay(file.toRequest()) })
            }
        }
    }
}

@Composable
private fun FileRow(file: HistoryItem, onClick: () -> Unit) {
    val context = LocalContext.current
    // File size is read lazily off the main thread; null while unknown or if access was lost.
    val size by produceState<Long?>(initialValue = null, file.source) {
        value = withContext(Dispatchers.IO) { querySize(context, Uri.parse(file.source)) }
    }
    val details = listOfNotNull(
        size?.let { Formatter.formatShortFileSize(context, it) },
        file.durationMs.takeIf { it > 0 }?.let { formatTime(it) }
    ).joinToString(" • ").ifEmpty { "Video file" }

    VideoRow(title = file.title, details = details, progress = 0f, onClick = onClick)
}

private fun displayName(context: Context, uri: Uri): String {
    val name = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }
    return name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "Local video"
}

private fun querySize(context: Context, uri: Uri): Long? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }
} catch (_: Exception) {
    null
}
