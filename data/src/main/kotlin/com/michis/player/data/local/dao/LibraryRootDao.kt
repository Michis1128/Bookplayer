package com.michis.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michis.player.data.local.entity.LibraryRootEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryRootDao {
    @Query("SELECT * FROM library_roots ORDER BY displayName COLLATE NOCASE") fun observeAll(): Flow<List<LibraryRootEntity>>
    @Upsert suspend fun upsert(root: LibraryRootEntity)
    @Query("DELETE FROM library_roots WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM library_roots WHERE id = :id LIMIT 1") suspend fun findById(id: String): LibraryRootEntity?
}
