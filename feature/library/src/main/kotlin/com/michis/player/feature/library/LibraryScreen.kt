package com.michis.player.feature.library

import androidx.compose.runtime.Composable
import com.michis.player.core.ui.component.EmptyState

data class LibraryUiState(val isLoading: Boolean = false, val bookCount: Int = 0)
sealed interface LibraryUiEvent { data object SelectLibrary : LibraryUiEvent }

@Composable
fun LibraryScreen(state: LibraryUiState, onEvent: (LibraryUiEvent) -> Unit) {
    EmptyState(
        title = if (state.isLoading) "Escaneando biblioteca…" else "Tu biblioteca está vacía",
        message = "Selecciona una carpeta con tus audiolibros para comenzar.",
        actionLabel = "Seleccionar carpeta",
        onAction = { onEvent(LibraryUiEvent.SelectLibrary) },
    )
}
