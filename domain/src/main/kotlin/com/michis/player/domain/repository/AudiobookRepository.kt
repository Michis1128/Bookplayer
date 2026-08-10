package com.michis.player.domain.repository

import com.michis.player.domain.model.Audiobook
import kotlinx.coroutines.flow.Flow

interface AudiobookRepository {
    fun observeAudiobooks(): Flow<List<Audiobook>>
    fun observeAudiobook(id: String): Flow<Audiobook?>
}
