package com.michis.player.feature.bookdetails

import androidx.compose.runtime.Composable
import com.michis.player.core.ui.component.EmptyState

@Composable fun BookDetailsScreen(bookId: String, onPlay: () -> Unit) =
    EmptyState("Detalle del audiolibro", "Libro: $bookId", "Reproducir", onPlay)
