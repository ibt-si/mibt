// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.interferolog.mibt.domain.CurrencyChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CurrencyMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MibtDatabase::class.java,
    )

    @Test
    fun migrate2To3_keepsOldAmountsAndAddsCurrencySettings() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO cycles (id, startedAt, completedAt, routeKind, weekOrder, status, fullCycleCompleted, vacation)
                VALUES (1, 0, NULL, 'BASIC', '1', 'ACTIVE', 0, 0)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO spends (cycleId, atMillis, node, amountKopecks, note, cleanSheet)
                VALUES (1, 0, 'U1', 470000, 'корзина', 0)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        migrated.query("SELECT currency FROM cycles WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("RUB", cursor.getString(0))
        }
        migrated.query("SELECT amountKopecks FROM spends WHERE cycleId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(470_000L, cursor.getLong(0))
        }
        migrated.query("SELECT id, currencyCode FROM app_settings").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(CurrencyChoice.systemDefault().code, cursor.getString(1))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DB = "currency-migration-test"
    }
}
