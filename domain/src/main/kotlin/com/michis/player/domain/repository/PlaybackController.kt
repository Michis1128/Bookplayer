package com.michis.player.domain.repository

import com.michis.player.domain.model.PlaybackSnapshot
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val state: StateFlow<PlaybackSnapshot>
    suspend fun playBook(bookId: String)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(offsetMs: Long)
}
