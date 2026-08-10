package com.michis.player.domain.model

data class Chapter(
    val id: String,
    val bookId: String,
    val audioFileId: String?,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val order: Int,
)
