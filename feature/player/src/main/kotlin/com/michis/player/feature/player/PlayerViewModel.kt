package com.michis.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michis.player.domain.repository.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(private val controller: PlaybackController) : ViewModel() {
    val state = controller.state.map { snapshot ->
        PlayerUiState(
            currentBook = snapshot.book,
            currentFile = snapshot.currentFile,
            isPlaying = snapshot.isPlaying,
            currentPositionMs = snapshot.currentPositionMs,
            durationMs = snapshot.durationMs,
            isBuffering = snapshot.isBuffering,
            playbackError = snapshot.playbackError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun playBook(bookId: String) { viewModelScope.launch { controller.playBook(bookId) } }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.TogglePlayback -> if (state.value.isPlaying) controller.pause() else controller.play()
            is PlayerUiEvent.SeekTo -> controller.seekTo(event.positionMs)
            PlayerUiEvent.SkipBackward -> controller.seekBy(-10_000L)
            PlayerUiEvent.SkipForward -> controller.seekBy(30_000L)
        }
    }
}
