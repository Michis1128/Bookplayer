package com.michis.player.domain.repository

import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.model.LibraryScanProgress
import kotlinx.coroutines.flow.Flow

interface LibraryScanner {
    fun scan(root: LibraryRoot): Flow<LibraryScanProgress>
}
