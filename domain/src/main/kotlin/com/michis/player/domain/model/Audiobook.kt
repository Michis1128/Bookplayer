package com.michis.player.domain.model

data class Audiobook(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val durationMs: Long,
    val status: BookStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val availability: BookAvailability = BookAvailability.AVAILABLE,
)

enum class BookStatus { NEW, IN_PROGRESS, COMPLETED }

enum class BookAvailability { AVAILABLE, UNAVAILABLE }
