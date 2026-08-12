package com.michis.player.feature.library

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michis.player.core.ui.component.EmptyState
import com.michis.player.core.ui.theme.LocalMichisSpacing
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.AudioFile
import com.michis.player.domain.model.BookAvailability
import com.michis.player.domain.model.BookStatus
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LibraryFilter { ALL, NEW, IN_PROGRESS, COMPLETED }
enum class LibraryViewMode { GRID, LIST }
enum class LibrarySortOption { TITLE, AUTHOR, DATE_ADDED, PROGRESS, LAST_PLAYED }

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Discovering : ScanUiState
    data class Scanning(val processed: Int, val total: Int, val name: String?) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class LibraryUiState(
    val books: List<LibraryBook> = emptyList(),
    val continueListening: List<LibraryBook> = emptyList(),
    val hasLibraryRoot: Boolean = false,
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val sort: LibrarySortOption = LibrarySortOption.TITLE,
    val scan: ScanUiState = ScanUiState.Idle,
)

data class LibraryBook(val book: Audiobook, val files: List<AudioFile> = emptyList())

sealed interface LibraryUiEvent {
    data class TreeSelected(val uri: String) : LibraryUiEvent
    data class QueryChanged(val query: String) : LibraryUiEvent
    data class FilterChanged(val filter: LibraryFilter) : LibraryUiEvent
    data class SortChanged(val sort: LibrarySortOption) : LibraryUiEvent
    data object ToggleViewMode : LibraryUiEvent
    data object Rescan : LibraryUiEvent
    data class RemoveFromLibrary(val bookId: String) : LibraryUiEvent
}

@Composable
fun LibraryRoute(
    onOpenBook: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onEvent(LibraryUiEvent.TreeSelected(it.toString())) }
    }
    LibraryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onSelectLibrary = { picker.launch(null) },
        onOpenBook = onOpenBook,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryUiEvent) -> Unit,
    onSelectLibrary: () -> Unit,
    onOpenBook: (String) -> Unit,
) {
    val spacing = LocalMichisSpacing.current
    var pendingRemoval by remember { mutableStateOf<LibraryBook?>(null) }
    Column(Modifier.fillMaxSize()) {
        LibraryHeader(state, onEvent, onSelectLibrary)
        ScanStatus(state.scan)
        if (state.continueListening.isNotEmpty()) ContinueListening(state.continueListening, onOpenBook) { pendingRemoval = it }
        if (state.books.isEmpty()) {
            EmptyState(
                title = if (state.hasLibraryRoot) "No se encontraron audiolibros" else "Tu biblioteca está vacía",
                message = if (state.hasLibraryRoot) "Agrega archivos compatibles o vuelve a escanear." else "Selecciona una carpeta con tus audiolibros para comenzar.",
                actionLabel = if (state.hasLibraryRoot) "Volver a escanear" else "Seleccionar carpeta",
                onAction = if (state.hasLibraryRoot) ({ onEvent(LibraryUiEvent.Rescan) }) else onSelectLibrary,
            )
        } else if (state.viewMode == LibraryViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) { items(state.books, key = { it.book.id }) { BookGridCard(it, onOpenBook) { pendingRemoval = it } } }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) { items(state.books, key = { it.book.id }) { BookListCard(it, onOpenBook) { pendingRemoval = it } } }
        }
    }
    pendingRemoval?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Quitar de la biblioteca") },
            text = { Text("¿Quieres quitar “${item.book.title}” de la biblioteca? Los archivos de audio permanecerán en el dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(LibraryUiEvent.RemoveFromLibrary(item.book.id))
                    pendingRemoval = null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LibraryHeader(state: LibraryUiState, onEvent: (LibraryUiEvent) -> Unit, onSelectLibrary: () -> Unit) {
    val spacing = LocalMichisSpacing.current
    Column(Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Biblioteca", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onSelectLibrary) { Text("Añadir carpeta") }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { onEvent(LibraryUiEvent.QueryChanged(it)) },
            label = { Text("Buscar por título o autor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            items(LibraryFilter.entries.size) { index ->
                val filter = LibraryFilter.entries[index]
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onEvent(LibraryUiEvent.FilterChanged(filter)) },
                    label = { Text(filter.label) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { onEvent(LibraryUiEvent.ToggleViewMode) }) { Text(if (state.viewMode == LibraryViewMode.GRID) "Vista lista" else "Vista cuadrícula") }
            Button(onClick = { onEvent(LibraryUiEvent.SortChanged(state.sort.next())) }) { Text("Orden: ${state.sort.label}") }
        }
    }
}

@Composable private fun ContinueListening(books: List<LibraryBook>, onOpen: (String) -> Unit, onRemove: (LibraryBook) -> Unit) {
    val spacing = LocalMichisSpacing.current
    Column(Modifier.fillMaxWidth().padding(bottom = spacing.medium)) {
        Text("Continuar escuchando", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small))
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.medium), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            items(books, key = { it.book.id }) { item ->
                val book = item.book
                Card(Modifier.height(96.dp).fillParentMaxWidth(0.72f).combinedClickable(onClick = { onOpen(book.id) }, onLongClick = { onRemove(item) })) {
                    Row(Modifier.padding(8.dp)) {
                        BookCover(book, Modifier.height(80.dp).aspectRatio(0.72f))
                        BookLabels(book, Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun ScanStatus(scan: ScanUiState) {
    when (scan) {
        ScanUiState.Idle -> Unit
        ScanUiState.Discovering -> Column { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Descubriendo archivos…", Modifier.padding(8.dp)) }
        is ScanUiState.Scanning -> Column {
            LinearProgressIndicator(progress = { if (scan.total == 0) 0f else scan.processed.toFloat() / scan.total }, modifier = Modifier.fillMaxWidth())
            Text("Escaneando biblioteca… ${scan.processed} / ${scan.total}${scan.name?.let { " · $it" } ?: ""}", Modifier.padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        is ScanUiState.Error -> Text(scan.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
    }
}

@Composable private fun BookGridCard(item: LibraryBook, onOpen: (String) -> Unit, onRemove: (LibraryBook) -> Unit) {
    val book = item.book
    Card(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(book.id) }, onLongClick = { onRemove(item) })) {
        Column {
            BookCover(book, Modifier.fillMaxWidth().aspectRatio(0.72f))
            BookLabels(book, Modifier.padding(12.dp))
            AudioFileList(item.files, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable private fun BookListCard(item: LibraryBook, onOpen: (String) -> Unit, onRemove: (LibraryBook) -> Unit) {
    val book = item.book
    Card(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(book.id) }, onLongClick = { onRemove(item) })) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.height(88.dp).aspectRatio(0.72f))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                BookLabels(book)
                AudioFileList(item.files, Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable private fun AudioFileList(files: List<AudioFile>, modifier: Modifier = Modifier) {
    if (files.size <= 1) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${files.size} archivos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        files.forEachIndexed { index, file ->
            Text(
                "${index + 1}. ${file.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable private fun BookLabels(book: Audiobook, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author ?: "Autor desconocido", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (book.availability == BookAvailability.AVAILABLE) book.status.label else "No disponible", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable private fun BookCover(book: Audiobook, modifier: Modifier) {
    var bitmap by remember(book.coverUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(book.coverUri) {
        bitmap = withContext(Dispatchers.IO) {
            book.coverUri?.let { runCatching { decodeSampledCover(File(URI(it)), 512) }.getOrNull() }
        }
    }
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), book.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(book.title.take(1).uppercase(), style = MaterialTheme.typography.displayMedium) }
    }
}

private val LibraryFilter.label get() = when (this) { LibraryFilter.ALL -> "Todos"; LibraryFilter.NEW -> "Nuevos"; LibraryFilter.IN_PROGRESS -> "En curso"; LibraryFilter.COMPLETED -> "Terminados" }
private val LibrarySortOption.label get() = when (this) { LibrarySortOption.TITLE -> "Título"; LibrarySortOption.AUTHOR -> "Autor"; LibrarySortOption.DATE_ADDED -> "Fecha"; LibrarySortOption.PROGRESS -> "Progreso"; LibrarySortOption.LAST_PLAYED -> "Última reproducción" }
private fun LibrarySortOption.next() = LibrarySortOption.entries[(ordinal + 1) % LibrarySortOption.entries.size]
private val BookStatus.label get() = when (this) { BookStatus.NEW -> "Nuevo"; BookStatus.IN_PROGRESS -> "En curso"; BookStatus.COMPLETED -> "Terminado" }

private fun decodeSampledCover(file: File, targetSize: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (bounds.outWidth / sample > targetSize * 2 || bounds.outHeight / sample > targetSize * 2) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}
