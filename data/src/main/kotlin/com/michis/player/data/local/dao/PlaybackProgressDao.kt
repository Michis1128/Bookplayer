package com.michis.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michis.player.data.local.entity.PlaybackProgressEntity

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId") suspend fun findByBook(bookId: String): PlaybackProgressEntity?
    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC LIMIT 1") suspend fun findLatest(): PlaybackProgressEntity?
    @Upsert suspend fun upsert(progress: PlaybackProgressEntity)
}
