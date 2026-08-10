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

    @Upsert suspend fun upsert(audiobook: AudiobookEntity)
}
