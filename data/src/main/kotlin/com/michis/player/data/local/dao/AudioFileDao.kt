package com.michis.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michis.player.data.local.entity.AudioFileEntity

@Dao
interface AudioFileDao {
    @Query("SELECT * FROM audio_files ORDER BY bookId, `order`") fun observeAll(): kotlinx.coroutines.flow.Flow<List<AudioFileEntity>>
    @Query("SELECT * FROM audio_files WHERE uri = :uri LIMIT 1") suspend fun findByUri(uri: String): AudioFileEntity?
    @Query("SELECT * FROM audio_files WHERE bookId = :bookId ORDER BY `order`") suspend fun findByBook(bookId: String): List<AudioFileEntity>
    @Upsert suspend fun upsertAll(files: List<AudioFileEntity>)
    @Query("DELETE FROM audio_files WHERE bookId = :bookId AND id NOT IN (:availableIds)") suspend fun deleteMissing(bookId: String, availableIds: Set<String>)
}
