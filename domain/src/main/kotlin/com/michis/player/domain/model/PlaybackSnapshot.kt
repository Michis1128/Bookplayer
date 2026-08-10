package com.michis.player.domain.model

data class PlaybackSnapshot(
    val book: Audiobook? = null,
    val currentFile: AudioFile? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val playbackError: String? = null,
)
