package br.com.tec.tecmotors

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.tec.tecmotors.data.local.migration.RoomMigrations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentedTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        "br.com.tec.tecmotors.data.local.TecMotorsDatabase",
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate3To4_backfillsVehicleBudgetsTable() {
        val dbName = "migration-test-3-4"

        helper.createDatabase(dbName, 3).apply {
            execSQL(
                """
                INSERT INTO settings (
                    id, darkThemeEnabled, legacyImportDone, dataUpdatedAtMillis, monthlyBudgetCar, monthlyBudgetMotorcycle
                ) VALUES (1, 1, 1, 123456, 420.0, 170.0)
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            RoomMigrations.MIGRATION_3_4
        )

        migratedDb.query("SELECT vehicleType, amount FROM vehicle_budgets ORDER BY vehicleType").use { cursor ->
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals("CAR", cursor.getString(0))
            assertEquals(420.0, cursor.getDouble(1), 0.001)

            cursor.moveToNext()
            assertEquals("MOTORCYCLE", cursor.getString(0))
            assertEquals(170.0, cursor.getDouble(1), 0.001)
        }
    }
}
