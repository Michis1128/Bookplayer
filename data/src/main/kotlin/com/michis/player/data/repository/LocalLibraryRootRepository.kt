package com.michis.player.data.repository

import com.michis.player.data.local.dao.LibraryRootDao
import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.repository.LibraryRootRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalLibraryRootRepository @Inject constructor(private val dao: LibraryRootDao) : LibraryRootRepository {
    override fun observeRoots(): Flow<List<LibraryRoot>> = dao.observeAll().map { roots ->
        roots.map { LibraryRoot(it.id, it.treeUri, it.displayName) }
    }
}
