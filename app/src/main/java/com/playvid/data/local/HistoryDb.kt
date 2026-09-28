package com.playvid.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Unique index on [source]: playing the same video again replaces its old row. */
@Entity(tableName = "history", indices = [Index(value = ["source"], unique = true)])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val source: String,
    val isLocal: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE source = :source LIMIT 1")
    suspend fun getBySource(source: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}

/** A saved playlist entry (from a picked .m3u/.m3u8 file or a pasted URL). */
@Entity(tableName = "playlist")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val addedAt: Long
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist ORDER BY addedAt ASC, id ASC")
    fun observeAll(): Flow<List<PlaylistItemEntity>>

    @Insert
    suspend fun insertAll(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM playlist")
    suspend fun clear()
}

@Database(entities = [HistoryEntity::class, PlaylistItemEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
}
