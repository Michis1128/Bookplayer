package com.michis.player.domain.model

data class PlaybackSnapshot(
    val book: Audiobook? = null,
    val currentFile: AudioFile? = null,
    val audioFiles: List<AudioFile> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val chapters: List<Chapter> = emptyList(),
    val playbackSpeed: Float = 1f,
    val isBuffering: Boolean = false,
    val playbackError: String? = null,
)
