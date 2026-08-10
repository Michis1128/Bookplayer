package com.michis.player.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michis.player.core.ui.theme.LocalMichisSpacing
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.Audiobook

data class PlayerUiState(
    val currentBook: Audiobook? = null,
    val currentFile: AudioFile? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val playbackError: String? = null,
)

sealed interface PlayerUiEvent {
    data object TogglePlayback : PlayerUiEvent
    data class SeekTo(val positionMs: Long) : PlayerUiEvent
    data object SkipBackward : PlayerUiEvent
    data object SkipForward : PlayerUiEvent
}

@Composable
fun PlayerRoute(bookId: String?, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { if (!bookId.isNullOrBlank()) viewModel.playBook(bookId) }
    PlayerScreen(state, viewModel::onEvent)
}

@Composable
fun MiniPlayerRoute(onOpenPlayer: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.currentBook != null) MiniPlayer(state, viewModel::onEvent, onOpenPlayer)
}

@Composable
fun PlayerScreen(state: PlayerUiState, onEvent: (PlayerUiEvent) -> Unit) {
    val spacing = LocalMichisSpacing.current
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth(0.72f).heightIn(min = 220.dp, max = 360.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.currentBook?.title?.take(1)?.uppercase() ?: "M", style = MaterialTheme.typography.displayLarge)
                }
            }
        }
        Text(state.currentBook?.title ?: "Ningún audiolibro seleccionado", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = spacing.large), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(state.currentBook?.author ?: "Autor desconocido", style = MaterialTheme.typography.bodyLarge)
        Text(state.currentFile?.name ?: "", modifier = Modifier.padding(top = spacing.medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Slider(
            value = (pendingSeekMs ?: state.currentPositionMs).coerceAtMost(state.durationMs).toFloat(),
            onValueChange = { pendingSeekMs = it.toLong() },
            onValueChangeFinished = {
                pendingSeekMs?.let { onEvent(PlayerUiEvent.SeekTo(it)) }
                pendingSeekMs = null
            },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            enabled = state.currentFile != null,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.medium),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.currentPositionMs))
            Text("−${formatDuration((state.durationMs - state.currentPositionMs).coerceAtLeast(0L))}")
        }
        Row(
            Modifier.fillMaxWidth().padding(top = spacing.medium),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onEvent(PlayerUiEvent.SkipBackward) }, enabled = state.currentFile != null) { Text("−10 s") }
            Button(onClick = { onEvent(PlayerUiEvent.TogglePlayback) }, enabled = state.currentFile != null) { Text(if (state.isPlaying) "Pausar" else "Reproducir") }
            Button(onClick = { onEvent(PlayerUiEvent.SkipForward) }, enabled = state.currentFile != null) { Text("+30 s") }
        }
        if (state.isBuffering) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = spacing.medium))
        state.playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = spacing.medium)) }
    }
}

@Composable
private fun MiniPlayer(state: PlayerUiState, onEvent: (PlayerUiEvent) -> Unit, onOpen: () -> Unit) {
    val progress = if (state.durationMs > 0) state.currentPositionMs.toFloat() / state.durationMs else 0f
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.currentBook?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.currentFile?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(onClick = { onEvent(PlayerUiEvent.TogglePlayback) }) { Text(if (state.isPlaying) "Pausa" else "Play") }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainingSeconds) else "%d:%02d".format(minutes, remainingSeconds)
}
