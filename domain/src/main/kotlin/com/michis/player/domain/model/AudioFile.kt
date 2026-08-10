package com.michis.player.domain.model

data class AudioFile(
    val id: String,
    val bookId: String,
    val uri: String,
    val name: String,
    val order: Int,
    val durationMs: Long,
    val mimeType: String?,
    val sizeBytes: Long?,
    val lastModified: Long?,
    val playable: Boolean = true,
)
