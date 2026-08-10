package com.michis.player.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class AudioMetadata(
    val title: String?,
    val author: String?,
    val album: String?,
    val durationMs: Long,
    val discNumber: Int?,
    val trackNumber: Int?,
    val artwork: ByteArray?,
    val playable: Boolean,
)

class AndroidAudioMetadataReader @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun read(uri: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uri))
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).asIndex(),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).asIndex(),
                artwork = retriever.embeddedPicture,
                playable = true,
            )
        } catch (_: RuntimeException) {
            AudioMetadata(null, null, null, 0L, null, null, null, false)
        } finally {
            runCatching { retriever.release() }
        }
    }
}

private fun String?.asIndex(): Int? = this?.substringBefore('/')?.trim()?.toIntOrNull()
