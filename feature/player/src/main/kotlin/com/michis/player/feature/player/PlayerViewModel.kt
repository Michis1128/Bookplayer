package com.michis.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michis.player.domain.repository.PlaybackController
import com.michis.player.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controller: PlaybackController,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state = combine(controller.state, settingsRepository.settings) { snapshot, settings ->
        PlayerUiState(
            currentBook = snapshot.book,
            currentFile = snapshot.currentFile,
            isPlaying = snapshot.isPlaying,
            currentPositionMs = snapshot.currentPositionMs,
            durationMs = snapshot.durationMs,
            chapters = snapshot.chapters,
            playbackSpeed = snapshot.playbackSpeed,
            skipBackwardSeconds = settings.skipBackwardSeconds,
            skipForwardSeconds = settings.skipForwardSeconds,
            isBuffering = snapshot.isBuffering,
            playbackError = snapshot.playbackError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun playBook(bookId: String) { viewModelScope.launch { controller.playBook(bookId) } }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.TogglePlayback -> if (state.value.isPlaying) controller.pause() else controller.play()
            is PlayerUiEvent.SeekTo -> controller.seekTo(event.positionMs)
            PlayerUiEvent.SkipBackward -> controller.seekBy(-state.value.skipBackwardSeconds * 1_000L)
            PlayerUiEvent.SkipForward -> controller.seekBy(state.value.skipForwardSeconds * 1_000L)
        }
    }
}
