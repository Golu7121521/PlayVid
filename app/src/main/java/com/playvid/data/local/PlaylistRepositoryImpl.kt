package com.playvid.data.local

import com.playvid.domain.model.PlaylistEntry
import com.playvid.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepositoryImpl(private val dao: PlaylistDao) : PlaylistRepository {

    override fun observeAll(): Flow<List<PlaylistEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addAll(items: List<PlaylistEntry>) {
        val now = System.currentTimeMillis()
        dao.insertAll(items.map { PlaylistItemEntity(title = it.title, url = it.url, addedAt = now) })
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()

    private fun PlaylistItemEntity.toDomain() = PlaylistEntry(id = id, title = title, url = url)
}
