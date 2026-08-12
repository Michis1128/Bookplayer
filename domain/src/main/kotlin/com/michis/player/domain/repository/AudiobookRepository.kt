package com.michis.player.domain.repository

import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

interface AudiobookRepository {
    fun observeAudiobooks(): Flow<List<Audiobook>>
    fun observeAudiobook(id: String): Flow<Audiobook?>
    fun observeAudioFiles(): Flow<List<AudioFile>>
    suspend fun markMissingBooksUnavailable(rootId: String, availableBookIds: Set<String>)
}
