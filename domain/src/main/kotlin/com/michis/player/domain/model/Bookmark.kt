package com.michis.player.domain.model

data class Bookmark(
    val id: String,
    val bookId: String,
    val audioFileId: String?,
    val timestampMs: Long,
    val note: String,
    val createdAt: Long,
)
