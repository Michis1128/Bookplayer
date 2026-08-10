package com.michis.player.domain.usecase

import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.model.LibraryScanProgress
import com.michis.player.domain.repository.LibraryScanner
import kotlinx.coroutines.flow.Flow

class ScanLibraryUseCase(private val scanner: LibraryScanner) {
    operator fun invoke(root: LibraryRoot): Flow<LibraryScanProgress> = scanner.scan(root)
}
