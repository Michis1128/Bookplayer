package com.michis.player.playback.service

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.michis.player.domain.model.PlaybackProgress
import com.michis.player.domain.repository.PlaybackDataRepository
import com.michis.player.playback.EXTRA_BOOK_ID
import com.michis.player.playback.createMediaItems
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var repository: PlaybackDataRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var progressJob: Job? = null
    private var lastPausedAt: Long? = null
    private var wasPlaying = false
    private val progressMutex = Mutex()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = Media3AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        val contentOnlyMediaSource = DefaultMediaSourceFactory(DataSource.Factory { ContentDataSource(this) })
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(contentOnlyMediaSource)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(playerListener) }
        mediaSession = MediaSession.Builder(this, player).build()
        startPeriodicProgress()
        serviceScope.launch { restoreLastPosition() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        progressJob?.cancel()
        val finalItem = player.currentMediaItem
        val finalPosition = player.currentPosition.coerceAtLeast(0L)
        val finalCompleted = player.playbackState == Player.STATE_ENDED && !player.hasNextMediaItem()
        runBlocking(Dispatchers.IO) { persistPosition(finalItem, finalPosition, completed = finalCompleted) }
        mediaSession?.release()
        mediaSession = null
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && wasPlaying && player.currentMediaItem != null) {
                lastPausedAt = System.currentTimeMillis()
                saveCurrentProgress(completed = false)
            }
            wasPlaying = isPlaying
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                savePosition(newPosition.mediaItem, newPosition.positionMs, completed = false)
            } else if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                savePosition(oldPosition.mediaItem, oldPosition.positionMs, completed = false)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem != null) saveCurrentProgress(completed = false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) saveCurrentProgress(completed = !player.hasNextMediaItem())
        }

        override fun onPlayerError(error: PlaybackException) {
            saveCurrentProgress(completed = false)
        }
    }

    private fun startPeriodicProgress() {
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(5_000)
                if (player.isPlaying) saveCurrentProgress(completed = false)
            }
        }
    }

    private suspend fun restoreLastPosition() = withContext(Dispatchers.IO) {
        val progress = repository.getLatestProgress() ?: return@withContext
        val book = repository.getAudiobook(progress.bookId) ?: return@withContext
        val files = repository.getAudioFiles(progress.bookId)
        val items = createMediaItems(book, files)
        if (items.isEmpty()) return@withContext
        val index = items.indexOfFirst { it.mediaId == progress.audioFileId }.coerceAtLeast(0)
        lastPausedAt = progress.lastPausedAt
        withContext(Dispatchers.Main) {
            player.setMediaItems(items, index, progress.positionMs.coerceAtLeast(0L))
            player.prepare()
        }
    }

    private fun saveCurrentProgress(completed: Boolean) {
        savePosition(player.currentMediaItem, player.currentPosition.coerceAtLeast(0L), completed)
    }

    private fun savePosition(mediaItem: MediaItem?, positionMs: Long, completed: Boolean) {
        serviceScope.launch { persistPosition(mediaItem, positionMs, completed) }
    }

    private suspend fun persistPosition(mediaItem: MediaItem?, positionMs: Long, completed: Boolean) {
        val item = mediaItem ?: return
        val bookId = item.mediaMetadata.extras?.getString(EXTRA_BOOK_ID) ?: return
        progressMutex.withLock {
            withContext(Dispatchers.IO) {
                repository.saveProgress(
                    PlaybackProgress(
                        bookId = bookId,
                        audioFileId = item.mediaId,
                        positionMs = positionMs,
                        lastPausedAt = lastPausedAt,
                        completed = completed,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }
}
