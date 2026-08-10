package com.michis.player.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.michis.player.core.common.NaturalOrder
import com.michis.player.data.local.dao.AudioFileDao
import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.entity.AudioFileEntity
import com.michis.player.data.local.entity.AudiobookEntity
import com.michis.player.data.metadata.AndroidAudioMetadataReader
import com.michis.player.domain.model.BookAvailability
import com.michis.player.domain.model.BookStatus
import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.model.LibraryScanProgress
import com.michis.player.domain.repository.LibraryScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

class SafLibraryScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audiobookDao: AudiobookDao,
    private val audioFileDao: AudioFileDao,
    private val metadataReader: AndroidAudioMetadataReader,
) : LibraryScanner {
    override fun scan(root: LibraryRoot): Flow<LibraryScanProgress> = channelFlow {
        val job = launch(Dispatchers.IO) {
            try {
                send(LibraryScanProgress.Discovering)
                val treeUri = Uri.parse(root.treeUri)
                val hasPermission = context.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }
                if (!hasPermission) {
                    audiobookDao.markRootUnavailable(root.id, System.currentTimeMillis())
                    send(LibraryScanProgress.PermissionLost(root.id))
                    return@launch
                }
                val tree = DocumentFile.fromTreeUri(context, treeUri)
                if (tree == null || !tree.exists() || !tree.canRead()) {
                    audiobookDao.markRootUnavailable(root.id, System.currentTimeMillis())
                    send(LibraryScanProgress.StorageUnavailable(root.id))
                    return@launch
                }
                val folders = mutableListOf<DiscoveredFolder>()
                discover(tree, folders)
                val candidates = folders.flatMap(::toBookCandidates)
                val totalFiles = candidates.sumOf { it.audioFiles.size }
                var processed = 0
                val availableBookIds = mutableSetOf<String>()
                candidates.forEach { candidate ->
                    val bookId = stableId(candidate.sourceUri)
                    val extracted = candidate.audioFiles.map { document ->
                        val current = audioFileDao.findByUri(document.uri.toString())
                        val unchanged = current != null && current.sizeBytes == document.length().nullIfUnknown()
                            && current.lastModified == document.lastModified().nullIfUnknown()
                        val entity = if (unchanged) current else document.extract(bookId)
                        processed++
                        send(LibraryScanProgress.Scanning(processed, totalFiles, document.name))
                        entity
                    }.sortedWith(audioEntityComparator)
                        .mapIndexed { index, file -> file.copy(order = index) }

                    val existing = audiobookDao.findBySourceUri(candidate.sourceUri)
                    val now = System.currentTimeMillis()
                    val title = extracted.firstNotNullOfOrNull { it.album }?.takeIf(String::isNotBlank)
                        ?: candidate.displayName.ifBlank { "Audiolibro sin título" }
                    val author = extracted.firstNotNullOfOrNull { it.author?.takeIf(String::isNotBlank) }
                    val coverUri = cacheCover(bookId, candidate)
                        ?: existing?.coverUri
                    audiobookDao.upsert(
                        AudiobookEntity(
                            id = bookId,
                            title = title,
                            author = author,
                            coverUri = coverUri,
                            durationMs = extracted.sumOf { it.durationMs },
                            status = existing?.status ?: BookStatus.NEW.name,
                            availability = BookAvailability.AVAILABLE.name,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                            rootId = root.id,
                            sourceUri = candidate.sourceUri,
                        ),
                    )
                    audioFileDao.upsertAll(extracted)
                    val ids = extracted.mapTo(mutableSetOf()) { it.id }
                    if (ids.isNotEmpty()) audioFileDao.deleteMissing(bookId, ids)
                    availableBookIds += bookId
                }
                if (availableBookIds.isEmpty()) audiobookDao.markRootUnavailable(root.id, System.currentTimeMillis())
                else audiobookDao.markMissingUnavailable(root.id, availableBookIds, System.currentTimeMillis())
                send(LibraryScanProgress.Completed(candidates.size, processed))
            } catch (_: SecurityException) {
                audiobookDao.markRootUnavailable(root.id, System.currentTimeMillis())
                send(LibraryScanProgress.PermissionLost(root.id))
            } catch (error: Exception) {
                send(LibraryScanProgress.Failed(error.message ?: "No se pudo escanear la biblioteca"))
            }
        }
    }

    private fun discover(directory: DocumentFile, result: MutableList<DiscoveredFolder>) {
        val audio = mutableListOf<DocumentFile>()
        val images = mutableListOf<DocumentFile>()
        directory.listFiles().forEach { child ->
            when {
                child.isDirectory -> discover(child, result)
                child.isFile && child.isSupportedAudio() -> audio += child
                child.isFile && child.isSupportedImage() -> images += child
            }
        }
        if (audio.isNotEmpty()) result += DiscoveredFolder(directory, audio, images)
    }

    private fun toBookCandidates(folder: DiscoveredFolder): List<BookCandidate> {
        val m4b = folder.audio.filter { it.extension() == "m4b" }.map { file ->
            BookCandidate(file.nameWithoutExtension(), file.uri.toString(), listOf(file), folder.images)
        }
        val remaining = folder.audio.filterNot { it.extension() == "m4b" }
        return m4b + if (remaining.isNotEmpty()) listOf(
            BookCandidate(folder.directory.name ?: remaining.first().nameWithoutExtension(), folder.directory.uri.toString(), remaining, folder.images),
        ) else emptyList()
    }

    private fun DocumentFile.extract(bookId: String): AudioFileEntity {
        val metadata = metadataReader.read(uri.toString())
        metadata.artwork?.let { cacheEmbeddedCover(bookId, it) }
        return AudioFileEntity(
            id = stableId(uri.toString()), bookId = bookId, uri = uri.toString(), name = name ?: "Audio",
            order = 0, durationMs = metadata.durationMs, mimeType = type, sizeBytes = length().nullIfUnknown(),
            lastModified = lastModified().nullIfUnknown(), playable = metadata.playable, title = metadata.title,
            author = metadata.author, album = metadata.album, discNumber = metadata.discNumber, trackNumber = metadata.trackNumber,
        )
    }

    private fun cacheCover(bookId: String, candidate: BookCandidate): String? {
        val cached = File(File(context.cacheDir, "covers"), "$bookId.img")
        if (cached.exists()) return cached.toURI().toString()
        val source = candidate.images.sortedWith(compareBy<DocumentFile> { it.coverPriority() }.thenBy { it.name ?: "" })
            .firstOrNull()?.let { image -> context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() } }
        if (source == null) return null
        val directory = File(context.cacheDir, "covers").apply { mkdirs() }
        return File(directory, "$bookId.img").also { it.writeBytes(source) }.toURI().toString()
    }

    private fun cacheEmbeddedCover(bookId: String, bytes: ByteArray) {
        val directory = File(context.cacheDir, "covers").apply { mkdirs() }
        File(directory, "$bookId.img").writeBytes(bytes)
    }

    companion object {
        internal val supportedAudio = setOf("mp3", "aac", "m4a", "m4b", "flac", "ogg")
        internal val supportedImages = setOf("jpg", "jpeg", "png", "webp")
        val audioEntityComparator = compareBy<AudioFileEntity>({ it.discNumber ?: Int.MAX_VALUE }, { it.trackNumber ?: Int.MAX_VALUE })
            .thenBy { Regex("\\d+").find(it.name)?.value?.toLongOrNull() ?: Long.MAX_VALUE }
            .thenComparator { left, right -> NaturalOrder.compare(left.name, right.name) }
        fun stableId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString()
    }
}

private data class DiscoveredFolder(val directory: DocumentFile, val audio: List<DocumentFile>, val images: List<DocumentFile>)
private data class BookCandidate(val displayName: String, val sourceUri: String, val audioFiles: List<DocumentFile>, val images: List<DocumentFile>)
private fun DocumentFile.extension() = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
private fun DocumentFile.nameWithoutExtension() = name?.substringBeforeLast('.') ?: "Audiolibro"
private fun DocumentFile.isSupportedAudio() = extension() in SafLibraryScanner.supportedAudio
private fun DocumentFile.isSupportedImage() = extension() in SafLibraryScanner.supportedImages
private fun Long.nullIfUnknown(): Long? = takeIf { it > 0L }
private fun DocumentFile.coverPriority(): Int = when (name?.lowercase()) {
    "cover.jpg" -> 0; "cover.png" -> 1; "folder.jpg" -> 2; else -> 3
}
