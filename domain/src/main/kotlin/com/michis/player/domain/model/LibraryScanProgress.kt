package com.michis.player.domain.model

sealed interface LibraryScanProgress {
    data object Discovering : LibraryScanProgress
    data class Scanning(val processedFiles: Int, val totalFiles: Int, val currentName: String?) : LibraryScanProgress
    data class Completed(val booksFound: Int, val filesProcessed: Int) : LibraryScanProgress
    data class PermissionLost(val rootId: String) : LibraryScanProgress
    data class StorageUnavailable(val rootId: String) : LibraryScanProgress
    data class Failed(val message: String) : LibraryScanProgress
}
