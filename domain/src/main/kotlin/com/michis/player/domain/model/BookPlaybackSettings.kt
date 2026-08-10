package com.michis.player.domain.model

data class BookPlaybackSettings(
    val bookId: String,
    val playbackSpeed: Float,
    val autoRewindEnabled: Boolean,
    val equalizerEnabled: Boolean,
    val volumeBoostEnabled: Boolean,
)
