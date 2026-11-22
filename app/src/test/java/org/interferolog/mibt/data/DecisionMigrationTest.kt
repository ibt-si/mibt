// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DecisionMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MibtDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsRowsAndAddsEmptyDecision() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            insertV1Rows(db)
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        migrated.query("SELECT name, netIncomeKopecks, frictionScore, decision FROM income_sources").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Кафе", cursor.getString(0))
            assertEquals(10_000_000L, cursor.getLong(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals("", cursor.getString(3))
        }
        migrated.query("SELECT name, mark, decision FROM work_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Приём", cursor.getString(0))
            assertEquals("CREATE", cursor.getString(1))
            assertEquals("", cursor.getString(2))
        }
        migrated.close()
    }

    private fun insertV1Rows(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO cycles (id, startedAt, completedAt, routeKind, weekOrder, status, fullCycleCompleted, vacation)
            VALUES (1, 0, NULL, 'BASIC', '1', 'ACTIVE', 0, 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO income_sources (
                cycleId, name, netIncomeKopecks, compensatoryNoiseKopecks,
                contractHours, frictionHours, incomeShare, hoursShare, frictionScore
            ) VALUES (1, 'Кафе', 10000000, 0, 25, 0, 50, 40, 3)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO work_items (cycleId, name, resultShare, hours, hoursShare, frictionScore, mark)
            VALUES (1, 'Приём', 40, 20, 30, 4, 'CREATE')
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
