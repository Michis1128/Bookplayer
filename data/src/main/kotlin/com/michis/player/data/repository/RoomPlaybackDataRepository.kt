package com.michis.player.data.repository

import com.michis.player.data.local.dao.AudioFileDao
import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.dao.PlaybackProgressDao
import com.michis.player.data.local.entity.AudioFileEntity
import com.michis.player.data.local.entity.AudiobookEntity
import com.michis.player.data.local.entity.PlaybackProgressEntity
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.BookAvailability
import com.michis.player.domain.model.BookStatus
import com.michis.player.domain.model.PlaybackProgress
import com.michis.player.domain.repository.PlaybackDataRepository
import javax.inject.Inject

class RoomPlaybackDataRepository @Inject constructor(
    private val audiobooks: AudiobookDao,
    private val audioFiles: AudioFileDao,
    private val progress: PlaybackProgressDao,
) : PlaybackDataRepository {
    override suspend fun getAudiobook(bookId: String): Audiobook? = audiobooks.findById(bookId)?.asDomain()
    override suspend fun getAudioFiles(bookId: String): List<AudioFile> = audioFiles.findByBook(bookId).map(AudioFileEntity::asDomain)
    override suspend fun getProgress(bookId: String): PlaybackProgress? = progress.findByBook(bookId)?.asDomain()
    override suspend fun getLatestProgress(): PlaybackProgress? = progress.findLatest()?.asDomain()

    override suspend fun saveProgress(progress: PlaybackProgress) {
        this.progress.upsert(progress.asEntity())
        val status = when {
            progress.completed -> BookStatus.COMPLETED
            progress.positionMs > 0L -> BookStatus.IN_PROGRESS
            else -> BookStatus.NEW
        }
        audiobooks.updateStatus(progress.bookId, status.name, progress.updatedAt)
    }
}

private fun AudiobookEntity.asDomain() = Audiobook(
    id, title, author, coverUri, durationMs, BookStatus.valueOf(status), createdAt, updatedAt,
    BookAvailability.valueOf(availability),
)
private fun AudioFileEntity.asDomain() = AudioFile(id, bookId, uri, name, order, durationMs, mimeType, sizeBytes, lastModified, playable)
private fun PlaybackProgressEntity.asDomain() = PlaybackProgress(bookId, audioFileId, positionMs, lastPausedAt, completed, updatedAt)
private fun PlaybackProgress.asEntity() = PlaybackProgressEntity(bookId, audioFileId, positionMs, lastPausedAt, completed, updatedAt)
