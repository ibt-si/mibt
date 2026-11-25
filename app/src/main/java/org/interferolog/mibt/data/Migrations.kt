// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.interferolog.mibt.domain.CurrencyChoice

/** System-locale currency code, safe to embed in SQL. */
internal fun systemCurrencyCode(): String {
    val raw = CurrencyChoice.systemDefault().code
    return if (raw.matches(Regex("[A-Z0-9]+"))) raw else "RUB"
}

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

/**
 * Denominates each cycle in a currency and stores the choice for future cycles.
 * Old cycles receive rubles, so their recorded amounts keep their original meaning.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cycles ADD COLUMN currency TEXT NOT NULL DEFAULT 'RUB'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_settings` (" +
                "`id` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("INSERT OR REPLACE INTO `app_settings` (`id`, `currencyCode`) VALUES (1, '${systemCurrencyCode()}')")
    }
}
