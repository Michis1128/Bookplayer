package com.michis.player.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.michis.player.domain.model.Chapter

data class PlayerUiState(
    val currentBook: Audiobook? = null,
    val currentFile: AudioFile? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val chapters: List<Chapter> = emptyList(),
    val playbackSpeed: Float = 1f,
    val skipBackwardSeconds: Int = 10,
    val skipForwardSeconds: Int = 30,
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
fun PlayerPanelHandleRoute(
    expanded: Boolean,
    onToggle: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.currentBook != null) {
        IconButton(onClick = onToggle) {
            Icon(
                if (expanded) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
                contentDescription = if (expanded) "Ocultar reproductor" else "Mostrar reproductor",
            )
        }
    }
}

@Composable
fun PlayerScreen(state: PlayerUiState, onEvent: (PlayerUiEvent) -> Unit) {
    var pendingSeekMs by remember(state.currentFile?.id) { mutableStateOf<Long?>(null) }
    var showChapters by remember(state.currentFile?.id) { mutableStateOf(false) }
    val displayedPositionMs = (pendingSeekMs ?: state.currentPositionMs).coerceIn(0L, state.durationMs.coerceAtLeast(0L))
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxHeight < 600.dp) {
            CompactPlayerContent(state, displayedPositionMs, { pendingSeekMs = it }, {
                pendingSeekMs?.let { onEvent(PlayerUiEvent.SeekTo(it)) }
                pendingSeekMs = null
            }, onEvent, { showChapters = true })
        } else {
            PortraitPlayerContent(state, displayedPositionMs, { pendingSeekMs = it }, {
                pendingSeekMs?.let { onEvent(PlayerUiEvent.SeekTo(it)) }
                pendingSeekMs = null
            }, onEvent, { showChapters = true })
        }
    }
    if (showChapters && state.chapters.isNotEmpty()) {
        ChapterSheet(
            state = state,
            onDismiss = { showChapters = false },
            onChapterSelected = { chapter ->
                onEvent(PlayerUiEvent.SeekTo(chapter.startMs))
                showChapters = false
            },
        )
    }
}

@Composable
private fun PortraitPlayerContent(
    state: PlayerUiState,
    displayedPositionMs: Long,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onEvent: (PlayerUiEvent) -> Unit,
    onShowChapters: () -> Unit,
) {
    val spacing = LocalMichisSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookCover(state, Modifier.fillMaxWidth(0.72f).heightIn(min = 220.dp, max = 360.dp))
        BookInformation(state, Modifier.padding(top = spacing.large))
        Timeline(state, displayedPositionMs, onSeekChange, onSeekFinished, Modifier.padding(top = spacing.medium))
        PlaybackControls(state, onEvent, onShowChapters, Modifier.padding(top = spacing.medium))
        PlaybackStatus(state, Modifier.padding(top = spacing.medium))
    }
}

@Composable
private fun CompactPlayerContent(
    state: PlayerUiState,
    displayedPositionMs: Long,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onEvent: (PlayerUiEvent) -> Unit,
    onShowChapters: () -> Unit,
) {
    val spacing = LocalMichisSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.medium),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookCover(state, Modifier.size(104.dp))
            BookInformation(state, Modifier.weight(1f).padding(start = spacing.medium))
        }
        Timeline(state, displayedPositionMs, onSeekChange, onSeekFinished, Modifier.padding(top = spacing.small))
        PlaybackControls(state, onEvent, onShowChapters, Modifier.padding(top = spacing.extraSmall))
        PlaybackStatus(state, Modifier.padding(top = spacing.small))
    }
}

@Composable
private fun BookCover(state: PlayerUiState, modifier: Modifier) {
    Card(modifier.aspectRatio(1f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.currentBook?.title?.take(1)?.uppercase() ?: "M", style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun BookInformation(state: PlayerUiState, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            state.currentBook?.title ?: "Ningún audiolibro seleccionado",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(state.currentBook?.author ?: "Autor desconocido", style = MaterialTheme.typography.bodyLarge)
        Text(
            state.currentFile?.name ?: "",
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Timeline(
    state: PlayerUiState,
    displayedPositionMs: Long,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Slider(
            value = displayedPositionMs.toFloat(),
            onValueChange = { onSeekChange(it.toLong()) },
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            enabled = state.currentFile != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(displayedPositionMs))
            Text("−${formatDuration((state.durationMs - displayedPositionMs).coerceAtLeast(0L))}")
        }
    }
}

@Composable
private fun PlaybackControls(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    onShowChapters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(onClick = { onEvent(PlayerUiEvent.SkipBackward) }, enabled = state.currentFile != null) {
            Icon(Icons.Rounded.FastRewind, contentDescription = "Retroceder ${state.skipBackwardSeconds} segundos")
        }
        FilledIconButton(
            onClick = { onEvent(PlayerUiEvent.TogglePlayback) },
            enabled = state.currentFile != null,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(32.dp),
            )
        }
        FilledIconButton(onClick = { onEvent(PlayerUiEvent.SkipForward) }, enabled = state.currentFile != null) {
            Icon(Icons.Rounded.FastForward, contentDescription = "Avanzar ${state.skipForwardSeconds} segundos")
        }
    }
    if (state.chapters.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            androidx.compose.material3.TextButton(onClick = onShowChapters) {
                Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = null)
                Text("Capítulos", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onChapterSelected: (Chapter) -> Unit,
) {
    val currentChapter = state.chapters.indexOfLast { it.startMs <= state.currentPositionMs }.coerceAtLeast(0)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Capítulos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            itemsIndexed(state.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterSelected(chapter) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            chapter.title,
                            style = if (index == currentChapter) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                            color = if (index == currentChapter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(formatDuration(chapter.startMs), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackStatus(state: PlayerUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        if (state.isBuffering) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
            FilledIconButton(onClick = { onEvent(PlayerUiEvent.TogglePlayback) }) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                )
            }
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
