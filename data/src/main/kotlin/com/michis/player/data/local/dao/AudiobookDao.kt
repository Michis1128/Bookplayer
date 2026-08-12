package com.michis.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michis.player.data.local.entity.AudiobookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @Query("SELECT * FROM audiobooks ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    fun observeById(id: String): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    suspend fun findById(id: String): AudiobookEntity?

    @Upsert suspend fun upsert(audiobook: AudiobookEntity)

    @Query("SELECT * FROM audiobooks WHERE rootId = :rootId")
    suspend fun findByRoot(rootId: String): List<AudiobookEntity>

    @Query("SELECT * FROM audiobooks WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): AudiobookEntity?

    @Query("UPDATE audiobooks SET availability = 'UNAVAILABLE', updatedAt = :updatedAt WHERE rootId = :rootId AND id NOT IN (:availableBookIds)")
    suspend fun markMissingUnavailable(rootId: String, availableBookIds: Set<String>, updatedAt: Long)

    @Query("UPDATE audiobooks SET availability = 'UNAVAILABLE', updatedAt = :updatedAt WHERE rootId = :rootId")
    suspend fun markRootUnavailable(rootId: String, updatedAt: Long)

    @Query("UPDATE audiobooks SET status = :status, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateStatus(bookId: String, status: String, updatedAt: Long)

    @Query("DELETE FROM audiobooks WHERE id = :bookId")
    suspend fun delete(bookId: String)
}
