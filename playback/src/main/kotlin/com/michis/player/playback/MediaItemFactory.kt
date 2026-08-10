package com.michis.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook

const val EXTRA_BOOK_ID = "com.michis.player.BOOK_ID"

fun createMediaItems(book: Audiobook, files: List<AudioFile>): List<MediaItem> = files
    .filter(AudioFile::playable)
    .sortedBy(AudioFile::order)
    .map { file ->
        MediaItem.Builder()
            .setMediaId(file.id)
            .setUri(file.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(file.name)
                    .setArtist(book.author ?: "Autor desconocido")
                    .setAlbumTitle(book.title)
                    .setArtworkUri(book.coverUri?.let(Uri::parse))
                    .setExtras(Bundle().apply { putString(EXTRA_BOOK_ID, book.id) })
                    .build(),
            )
            .build()
    }
