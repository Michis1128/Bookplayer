package com.michis.player.playback.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.Chapter as Media3Chapter
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.Chapter
import com.michis.player.domain.model.PlaybackSnapshot
import com.michis.player.domain.repository.PlaybackController
import com.michis.player.domain.repository.PlaybackDataRepository
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.playback.EXTRA_BOOK_ID
import com.michis.player.playback.createMediaItems
import com.michis.player.playback.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PlaybackDataRepository,
    settingsRepository: SettingsRepository,
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerFuture: ListenableFuture<MediaController> = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()
    private val mutableState = MutableStateFlow(PlaybackSnapshot())
    override val state: StateFlow<PlaybackSnapshot> = mutableState.asStateFlow()
    private var cachedBook: Audiobook? = null
    private var cachedFiles: List<AudioFile> = emptyList()
    private var preferredSpeed = 1f

    init {
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                preferredSpeed = settings.playbackSpeed
                withController { it.setPlaybackSpeed(preferredSpeed) }
            }
        }
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { controller ->
                    controller.addListener(listener)
                    controller.setPlaybackSpeed(preferredSpeed)
                    refresh(controller)
                    startPositionUpdates(controller)
                }.onFailure { error -> mutableState.value = PlaybackSnapshot(playbackError = error.message ?: "No se pudo conectar al reproductor") }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override suspend fun playBook(bookId: String) {
        val controller = awaitController()
        val currentBookId = controller.currentMediaItem?.mediaMetadata?.extras?.getString(EXTRA_BOOK_ID)
        if (currentBookId == bookId && controller.mediaItemCount > 0) {
            controller.play()
            return
        }
        val (book, files, progress) = withContext(Dispatchers.IO) {
            Triple(repository.getAudiobook(bookId), repository.getAudioFiles(bookId), repository.getProgress(bookId))
        }
        if (book == null) {
            mutableState.value = mutableState.value.copy(playbackError = "El audiolibro ya no está disponible")
            return
        }
        val items = createMediaItems(book, files)
        if (items.isEmpty()) {
            mutableState.value = PlaybackSnapshot(book = book, playbackError = "No hay archivos reproducibles")
            return
        }
        cachedBook = book
        cachedFiles = files
        val index = items.indexOfFirst { it.mediaId == progress?.audioFileId }.coerceAtLeast(0)
        controller.setMediaItems(items, index, progress?.positionMs?.coerceAtLeast(0L) ?: 0L)
        controller.prepare()
        controller.setPlaybackSpeed(preferredSpeed)
        controller.play()
        refresh(controller)
    }

    override fun play() = withController(MediaController::play)
    override fun pause() = withController(MediaController::pause)
    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }
    override fun seekBy(offsetMs: Long) = withController { controller ->
        controller.seekTo((controller.currentPosition + offsetMs).coerceIn(0L, controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
    }
    override fun seekToFile(audioFileId: String) = withController { controller ->
        val index = (0 until controller.mediaItemCount).indexOfFirst { controller.getMediaItemAt(it).mediaId == audioFileId }
        if (index >= 0) controller.seekTo(index, 0L)
    }

    private fun withController(action: (MediaController) -> Unit) {
        if (controllerFuture.isDone) runCatching { controllerFuture.get() }.onSuccess(action)
    }

    private suspend fun awaitController(): MediaController = suspendCancellableCoroutine { continuation ->
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { continuation.resume(it) }
                    .onFailure { continuation.resumeWithException(it) }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
        override fun onPlayerError(error: PlaybackException) {
            mutableState.value = mutableState.value.copy(playbackError = error.message ?: "No se pudo reproducir el archivo")
        }
    }

    private fun refresh(player: Player) {
        scope.launch {
            val item = player.currentMediaItem
            val bookId = item?.mediaMetadata?.extras?.getString(EXTRA_BOOK_ID)
            if (bookId != null && cachedBook?.id != bookId) {
                cachedBook = withContext(Dispatchers.IO) { repository.getAudiobook(bookId) }
                cachedFiles = withContext(Dispatchers.IO) { repository.getAudioFiles(bookId) }
            }
            mutableState.value = PlaybackSnapshot(
                book = cachedBook,
                currentFile = cachedFiles.firstOrNull { it.id == item?.mediaId },
                audioFiles = cachedFiles,
                isPlaying = player.isPlaying,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { it > 0L } ?: cachedFiles.firstOrNull { it.id == item?.mediaId }?.durationMs ?: 0L,
                chapters = extractChapters(player, bookId, item?.mediaId),
                playbackSpeed = player.playbackParameters.speed,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                playbackError = player.playerError?.message,
            )
        }
    }

    private fun extractChapters(player: Player, bookId: String?, audioFileId: String?): List<Chapter> {
        if (bookId == null || audioFileId == null) return emptyList()
        return player.currentTracks.groups
            .asSequence()
            .flatMap { group -> (0 until group.length).asSequence().map { group.getTrackFormat(it) } }
            .mapNotNull { it.metadata }
            .flatMap { metadata -> (0 until metadata.length()).asSequence().map { metadata.get(it) } }
            .filterIsInstance<Media3Chapter>()
            .filterNot { it.isHidden }
            .distinctBy { it.startTimeMs }
            .sortedBy { it.startTimeMs }
            .mapIndexed { index, chapter ->
                Chapter(
                    id = "$audioFileId:${chapter.startTimeMs}",
                    bookId = bookId,
                    audioFileId = audioFileId,
                    title = chapter.title?.value ?: "Capítulo ${index + 1}",
                    startMs = chapter.startTimeMs.coerceAtLeast(0L),
                    endMs = chapter.endTimeMs.takeUnless { it == C.TIME_UNSET } ?: 0L,
                    order = index,
                )
            }
            .toList()
    }

    private fun startPositionUpdates(player: Player) {
        scope.launch {
            while (isActive) {
                delay(400)
                if (player.currentMediaItem != null) refresh(player)
            }
        }
    }
}
