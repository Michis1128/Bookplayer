package com.michis.player.data.repository

import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.entity.AudiobookEntity
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.BookAvailability
import com.michis.player.domain.model.BookStatus
import com.michis.player.domain.repository.AudiobookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalAudiobookRepository @Inject constructor(private val dao: AudiobookDao) : AudiobookRepository {
    override fun observeAudiobooks(): Flow<List<Audiobook>> = dao.observeAll().map { books -> books.map(AudiobookEntity::asDomain) }
    override fun observeAudiobook(id: String): Flow<Audiobook?> = dao.observeById(id).map { it?.asDomain() }
}

private fun AudiobookEntity.asDomain() = Audiobook(
    id, title, author, coverUri, durationMs, BookStatus.valueOf(status), createdAt, updatedAt,
    BookAvailability.valueOf(availability),
)
