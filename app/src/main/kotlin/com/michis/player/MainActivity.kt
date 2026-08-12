package com.michis.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michis.player.core.ui.theme.MichisTheme
import com.michis.player.domain.repository.GlobalSettings
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.domain.repository.PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playbackController: PlaybackController
    private var pictureInPicture by mutableStateOf(false)
    private var pipEnabled = true
    private var hasActiveBook = false
    private var isPlaying = false
    private var skipBackwardMs = 10_000L
    private var skipForwardMs = 30_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(GlobalSettings())
            val playback by playbackController.state.collectAsStateWithLifecycle()
            SideEffect {
                pipEnabled = settings.pictureInPictureEnabled
                hasActiveBook = playback.book != null
                isPlaying = playback.isPlaying
                skipBackwardMs = settings.skipBackwardSeconds * 1_000L
                skipForwardMs = settings.skipForwardSeconds * 1_000L
                updatePictureInPictureParams()
            }
            MichisTheme(theme = settings.theme) { MichisPlayerApp(pictureInPicture) }
        }
        handlePlaybackAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePlaybackAction(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipEnabled && hasActiveBook && !isInPictureInPictureMode) {
            enterPictureInPictureMode(buildPictureInPictureParams())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPicture = isInPictureInPictureMode
    }

    private fun updatePictureInPictureParams() {
        if (hasActiveBook) setPictureInPictureParams(buildPictureInPictureParams())
    }

    private fun buildPictureInPictureParams(): PictureInPictureParams = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .setActions(
            listOf(
                remoteAction(ACTION_BACK, android.R.drawable.ic_media_rew, "Retroceder"),
                remoteAction(if (isPlaying) ACTION_PAUSE else ACTION_PLAY, if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (isPlaying) "Pausar" else "Reproducir"),
                remoteAction(ACTION_FORWARD, android.R.drawable.ic_media_ff, "Avanzar"),
            ),
        )
        .build()

    private fun remoteAction(action: String, iconRes: Int, title: String): RemoteAction {
        val intent = Intent(this, MainActivity::class.java).setAction(action)
        val pendingIntent = PendingIntent.getActivity(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)
    }

    private fun handlePlaybackAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> playbackController.play()
            ACTION_PAUSE -> playbackController.pause()
            ACTION_BACK -> playbackController.seekBy(-skipBackwardMs)
            ACTION_FORWARD -> playbackController.seekBy(skipForwardMs)
        }
        if (intent?.action in playbackActions) intent?.action = null
    }

    private companion object {
        const val ACTION_PLAY = "com.michis.player.PIP_PLAY"
        const val ACTION_PAUSE = "com.michis.player.PIP_PAUSE"
        const val ACTION_BACK = "com.michis.player.PIP_BACK"
        const val ACTION_FORWARD = "com.michis.player.PIP_FORWARD"
        val playbackActions = setOf(ACTION_PLAY, ACTION_PAUSE, ACTION_BACK, ACTION_FORWARD)
    }
}
