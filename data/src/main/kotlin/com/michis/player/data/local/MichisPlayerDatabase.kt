package com.michis.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.dao.AudioFileDao
import com.michis.player.data.local.dao.LibraryRootDao
import com.michis.player.data.local.dao.PlaybackProgressDao
import com.michis.player.data.local.entity.*

@Database(
    entities = [LibraryRootEntity::class, AudiobookEntity::class, AudioFileEntity::class,
        ChapterEntity::class, PlaybackProgressEntity::class, BookmarkEntity::class,
        BookPlaybackSettingsEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MichisPlayerDatabase : RoomDatabase() {
    abstract fun audiobookDao(): AudiobookDao
    abstract fun audioFileDao(): AudioFileDao
    abstract fun libraryRootDao(): LibraryRootDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
}
