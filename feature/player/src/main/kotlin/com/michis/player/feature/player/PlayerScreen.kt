package com.michis.player.feature.player

import androidx.compose.runtime.Composable
import com.michis.player.core.ui.component.EmptyState

data class PlayerUiState(val isPlaying: Boolean = false, val currentPositionMs: Long = 0)
sealed interface PlayerUiEvent { data object TogglePlayback : PlayerUiEvent }

@Composable fun PlayerScreen(state: PlayerUiState, onEvent: (PlayerUiEvent) -> Unit) =
    EmptyState("Reproductor", "La reproducción se implementará en la Fase 3.", if (state.isPlaying) "Pausar" else "Reproducir") {
        onEvent(PlayerUiEvent.TogglePlayback)
    }
