package com.michis.player.domain.repository

import com.michis.player.domain.model.LibraryRoot
import kotlinx.coroutines.flow.Flow

interface LibraryRootRepository {
    fun observeRoots(): Flow<List<LibraryRoot>>
    suspend fun addRoot(treeUri: String): LibraryRoot
    suspend fun removeRoot(id: String)
}
