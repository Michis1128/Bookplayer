package com.michis.player.domain.repository

import com.michis.player.domain.model.LibraryRoot
import kotlinx.coroutines.flow.Flow

interface LibraryRootRepository {
    fun observeRoots(): Flow<List<LibraryRoot>>
}
