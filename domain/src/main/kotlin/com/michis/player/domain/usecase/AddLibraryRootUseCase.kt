package com.michis.player.domain.usecase

import com.michis.player.domain.model.LibraryScanProgress
import com.michis.player.domain.repository.LibraryRootRepository
import com.michis.player.domain.repository.LibraryScanner
import kotlinx.coroutines.flow.Flow

class AddLibraryRootUseCase(
    private val roots: LibraryRootRepository,
    private val scanner: LibraryScanner,
) {
    suspend operator fun invoke(treeUri: String): Flow<LibraryScanProgress> = scanner.scan(roots.addRoot(treeUri))
}
