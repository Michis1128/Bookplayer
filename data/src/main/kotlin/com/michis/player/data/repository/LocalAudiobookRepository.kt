package com.michis.player.data.repository

import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.dao.AudioFileDao
import com.michis.player.data.local.entity.AudiobookEntity
import com.michis.player.data.local.entity.AudioFileEntity
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.BookAvailability
import com.michis.player.domain.model.BookStatus
import com.michis.player.domain.repository.AudiobookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalAudiobookRepository @Inject constructor(
    private val dao: AudiobookDao,
    private val audioFileDao: AudioFileDao,
) : AudiobookRepository {
    override fun observeAudiobooks(): Flow<List<Audiobook>> = dao.observeAll().map { books -> books.map(AudiobookEntity::asDomain) }
    override fun observeAudiobook(id: String): Flow<Audiobook?> = dao.observeById(id).map { it?.asDomain() }
    override fun observeAudioFiles(): Flow<List<AudioFile>> = audioFileDao.observeAll().map { files -> files.map(AudioFileEntity::asDomain) }
    override suspend fun markMissingBooksUnavailable(rootId: String, availableBookIds: Set<String>) {
        if (availableBookIds.isEmpty()) dao.markRootUnavailable(rootId, System.currentTimeMillis())
        else dao.markMissingUnavailable(rootId, availableBookIds, System.currentTimeMillis())
    }
    override suspend fun removeFromLibrary(id: String) = dao.hide(id, System.currentTimeMillis())
}

private fun AudiobookEntity.asDomain() = Audiobook(
    id, title, author, coverUri, durationMs, BookStatus.valueOf(status), createdAt, updatedAt,
    BookAvailability.valueOf(availability),
)

private fun AudioFileEntity.asDomain() = AudioFile(id, bookId, uri, name, order, durationMs, mimeType, sizeBytes, lastModified, playable)
