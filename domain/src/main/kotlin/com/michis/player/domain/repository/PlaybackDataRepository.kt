package com.michis.player.domain.repository

import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.PlaybackProgress

interface PlaybackDataRepository {
    suspend fun getAudiobook(bookId: String): Audiobook?
    suspend fun getAudioFiles(bookId: String): List<AudioFile>
    suspend fun getProgress(bookId: String): PlaybackProgress?
    suspend fun getLatestProgress(): PlaybackProgress?
    suspend fun saveProgress(progress: PlaybackProgress)
}
