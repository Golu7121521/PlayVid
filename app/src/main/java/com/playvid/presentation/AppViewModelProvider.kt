package com.playvid.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.playvid.PlayVidApp
import com.playvid.presentation.downloads.DownloadsViewModel
import com.playvid.presentation.files.FilesViewModel
import com.playvid.presentation.home.HomeViewModel
import com.playvid.presentation.playlist.PlaylistsViewModel
import com.playvid.presentation.settings.SettingsViewModel

/** Gives ViewModel factories access to the Application (and its AppContainer). */
fun CreationExtras.app(): PlayVidApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlayVidApp

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.historyRepository, app().container.validateUrl) }
        initializer { FilesViewModel(app().container.historyRepository) }
        initializer { SettingsViewModel(app().container.settingsRepository, app().container.historyRepository) }
        initializer { DownloadsViewModel(app()) }
        initializer { PlaylistsViewModel(app(), app().container.playlistRepository) }
    }
}
