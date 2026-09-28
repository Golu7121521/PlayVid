package com.playvid.domain.repository

import com.playvid.domain.model.AppSettings
import com.playvid.domain.model.HistoryItem
import com.playvid.domain.model.PlaylistEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeAll(): Flow<List<HistoryItem>>
    suspend fun get(source: String): HistoryItem?
    suspend fun saveProgress(source: String, title: String, isLocal: Boolean, positionMs: Long, durationMs: Long)
    suspend fun delete(id: Long)
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface PlaylistRepository {
    fun observeAll(): Flow<List<PlaylistEntry>>
    suspend fun addAll(items: List<PlaylistEntry>)
    suspend fun delete(id: Long)
    suspend fun clear()
}
