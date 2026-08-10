package com.michis.player.data.repository

import com.michis.player.data.local.dao.LibraryRootDao
import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.repository.LibraryRootRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

class LocalLibraryRootRepository @Inject constructor(
    private val dao: LibraryRootDao,
    @param:ApplicationContext private val context: Context,
) : LibraryRootRepository {
    override fun observeRoots(): Flow<List<LibraryRoot>> = dao.observeAll().map { roots ->
        roots.map { LibraryRoot(it.id, it.treeUri, it.displayName) }
    }

    override suspend fun addRoot(treeUri: String): LibraryRoot {
        val uri = Uri.parse(treeUri)
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val name = DocumentFile.fromTreeUri(context, uri)?.name?.takeIf(String::isNotBlank) ?: "Biblioteca"
        val root = LibraryRoot(UUID.nameUUIDFromBytes(treeUri.toByteArray(StandardCharsets.UTF_8)).toString(), treeUri, name)
        dao.upsert(com.michis.player.data.local.entity.LibraryRootEntity(root.id, root.treeUri, root.displayName))
        return root
    }

    override suspend fun removeRoot(id: String) = dao.delete(id)
}
