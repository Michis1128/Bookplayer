package com.michis.player.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "library_roots")
data class LibraryRootEntity(@PrimaryKey val id: String, val treeUri: String, val displayName: String)

@Entity(tableName = "audiobooks", indices = [Index("rootId"), Index(value = ["sourceUri"], unique = true)])
data class AudiobookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val durationMs: Long,
    val status: String,
    val availability: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rootId: String?,
    val sourceUri: String?,
    val hidden: Boolean = false,
)

@Entity(
    tableName = "audio_files",
    foreignKeys = [ForeignKey(entity = AudiobookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId"), Index(value = ["uri"], unique = true)],
)
data class AudioFileEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val uri: String,
    val name: String,
    val order: Int,
    val durationMs: Long,
    val mimeType: String?,
    val sizeBytes: Long?,
    val lastModified: Long?,
    val playable: Boolean,
    val title: String?,
    val author: String?,
    val album: String?,
    val discNumber: Int?,
    val trackNumber: Int?,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(entity = AudiobookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId"), Index("audioFileId")],
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val audioFileId: String?,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val order: Int,
)

@Entity(
    tableName = "playback_progress",
    foreignKeys = [ForeignKey(entity = AudiobookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
)
data class PlaybackProgressEntity(
    @PrimaryKey val bookId: String,
    val audioFileId: String?,
    val positionMs: Long,
    val lastPausedAt: Long?,
    val completed: Boolean,
    val updatedAt: Long,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(entity = AudiobookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId"), Index("audioFileId")],
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val audioFileId: String?,
    val timestampMs: Long,
    val note: String,
    val createdAt: Long,
)

@Entity(
    tableName = "book_playback_settings",
    foreignKeys = [ForeignKey(entity = AudiobookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
)
data class BookPlaybackSettingsEntity(
    @PrimaryKey val bookId: String,
    val playbackSpeed: Float,
    val autoRewindEnabled: Boolean,
    val equalizerEnabled: Boolean,
    val volumeBoostEnabled: Boolean,
)
