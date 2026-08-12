package com.michis.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michis.player.domain.model.Audiobook
import com.michis.player.domain.model.BookStatus
import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.model.LibraryScanProgress
import com.michis.player.domain.repository.AudiobookRepository
import com.michis.player.domain.repository.LibraryRootRepository
import com.michis.player.domain.usecase.AddLibraryRootUseCase
import com.michis.player.domain.usecase.ScanLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val audiobooks: AudiobookRepository,
    roots: LibraryRootRepository,
    private val addLibraryRoot: AddLibraryRootUseCase,
    private val scanLibrary: ScanLibraryUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val viewMode = MutableStateFlow(LibraryViewMode.GRID)
    private val sort = MutableStateFlow(LibrarySortOption.TITLE)
    private val scan = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    private val rootsState = roots.observeRoots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var scanJob: Job? = null

    private val preferences = combine(query, filter, viewMode, sort) { query, filter, mode, sort ->
        Preferences(query, filter, mode, sort)
    }
    private val content = combine(audiobooks.observeAudiobooks(), audiobooks.observeAudioFiles(), rootsState) { books, files, libraryRoots ->
        val filesByBook = files.groupBy { it.bookId }
        Content(books.map { book -> LibraryBook(book, filesByBook[book.id].orEmpty()) }, libraryRoots)
    }

    val state = combine(content, preferences, scan) { content, preferences, scanState ->
        val visible = content.books.asSequence()
            .filter { item -> preferences.query.isBlank() || item.book.title.contains(preferences.query, true) || item.book.author?.contains(preferences.query, true) == true || item.files.any { it.name.contains(preferences.query, true) } }
            .filter { preferences.filter == LibraryFilter.ALL || it.book.status.name == preferences.filter.name }
            .sortedWith(preferences.sort.comparator)
            .toList()
        val listening = content.books.filter { it.book.status == BookStatus.IN_PROGRESS }.sortedByDescending { it.book.updatedAt }
        LibraryUiState(visible, listening, content.roots.isNotEmpty(), preferences.query, preferences.filter, preferences.mode, preferences.sort, scanState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            val firstRoot = rootsState.filter { it.isNotEmpty() }.first().first()
            startScan { scanLibrary(firstRoot) }
        }
    }

    fun onEvent(event: LibraryUiEvent) {
        when (event) {
            is LibraryUiEvent.QueryChanged -> query.value = event.query
            is LibraryUiEvent.FilterChanged -> filter.value = event.filter
            is LibraryUiEvent.SortChanged -> sort.value = event.sort
            LibraryUiEvent.ToggleViewMode -> viewMode.value = if (viewMode.value == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
            is LibraryUiEvent.TreeSelected -> startScan { addLibraryRoot(event.uri) }
            LibraryUiEvent.Rescan -> rootsState.value.firstOrNull()?.let { root -> startScan { scanLibrary(root) } }
            is LibraryUiEvent.RemoveFromLibrary -> viewModelScope.launch { audiobooks.removeFromLibrary(event.bookId) }
        }
    }

    private fun startScan(flow: suspend () -> kotlinx.coroutines.flow.Flow<LibraryScanProgress>) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            flow().collect { progress -> scan.value = progress.asUiState() }
        }
    }
}

private data class Preferences(val query: String, val filter: LibraryFilter, val mode: LibraryViewMode, val sort: LibrarySortOption)
private data class Content(val books: List<LibraryBook>, val roots: List<LibraryRoot>)
private val LibrarySortOption.comparator: Comparator<LibraryBook> get() = when (this) {
    LibrarySortOption.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.book.title }
    LibrarySortOption.AUTHOR -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.book.author ?: "" }
    LibrarySortOption.DATE_ADDED -> compareByDescending { it.book.createdAt }
    LibrarySortOption.PROGRESS -> compareBy { when (it.book.status) { BookStatus.IN_PROGRESS -> 0; BookStatus.NEW -> 1; BookStatus.COMPLETED -> 2 } }
    LibrarySortOption.LAST_PLAYED -> compareByDescending { it.book.updatedAt }
}
private fun LibraryScanProgress.asUiState(): ScanUiState = when (this) {
    LibraryScanProgress.Discovering -> ScanUiState.Discovering
    is LibraryScanProgress.Scanning -> ScanUiState.Scanning(processedFiles, totalFiles, currentName)
    is LibraryScanProgress.Completed -> ScanUiState.Idle
    is LibraryScanProgress.PermissionLost -> ScanUiState.Error("Se perdió el permiso de acceso a la biblioteca.")
    is LibraryScanProgress.StorageUnavailable -> ScanUiState.Error("El almacenamiento de la biblioteca no está disponible.")
    is LibraryScanProgress.Failed -> ScanUiState.Error(message)
}
