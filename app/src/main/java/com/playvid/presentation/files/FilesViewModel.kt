package com.playvid.presentation.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playvid.domain.model.HistoryItem
import com.playvid.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FilesViewModel(historyRepo: HistoryRepository) : ViewModel() {

    /** Local videos the user has opened before. null = still loading. */
    val files: StateFlow<List<HistoryItem>?> = historyRepo.observeAll()
        .map { list -> list.filter { it.isLocal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
