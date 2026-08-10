package com.michis.player.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE audiobooks ADD COLUMN rootId TEXT")
        db.execSQL("ALTER TABLE audiobooks ADD COLUMN sourceUri TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_audiobooks_rootId ON audiobooks(rootId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_audiobooks_sourceUri ON audiobooks(sourceUri)")
        db.execSQL("ALTER TABLE audio_files ADD COLUMN title TEXT")
        db.execSQL("ALTER TABLE audio_files ADD COLUMN author TEXT")
        db.execSQL("ALTER TABLE audio_files ADD COLUMN album TEXT")
        db.execSQL("ALTER TABLE audio_files ADD COLUMN discNumber INTEGER")
        db.execSQL("ALTER TABLE audio_files ADD COLUMN trackNumber INTEGER")
    }
}
