// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the editable decision text to income sources and work items.
 * Existing rows keep every column they already had and receive an empty decision.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE income_sources ADD COLUMN decision TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_items ADD COLUMN decision TEXT NOT NULL DEFAULT ''")
    }
}
