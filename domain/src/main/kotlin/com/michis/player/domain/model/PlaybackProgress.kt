package com.michis.player.domain.model

data class PlaybackProgress(
    val bookId: String,
    val audioFileId: String?,
    val positionMs: Long,
    val lastPausedAt: Long?,
    val completed: Boolean,
    val updatedAt: Long,
)
