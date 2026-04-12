package br.com.tec.tecmotors.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateVehicles(db)
            recreateOdometerRecords(db)
            recreateFuelRecords(db)
            recreateMaintenanceRecords(db)
            recreateSettings(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "fuel_records", "stationName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "fuel_records", "usageType", "TEXT NOT NULL DEFAULT 'MIXED'")
            addColumnIfMissing(db, "fuel_records", "receiptImageUri", "TEXT")

            addColumnIfMissing(db, "maintenance_records", "receiptImageUri", "TEXT")

            addColumnIfMissing(db, "settings", "monthlyBudgetCar", "REAL NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "settings", "monthlyBudgetMotorcycle", "REAL NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicle_budgets` (
                  `vehicleType` TEXT NOT NULL,
                  `amount` REAL NOT NULL,
                  PRIMARY KEY(`vehicleType`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR REPLACE INTO `vehicle_budgets` (`vehicleType`, `amount`)
                SELECT 'CAR', COALESCE(`monthlyBudgetCar`, 0)
                FROM `settings`
                WHERE `id` = 1
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO `vehicle_budgets` (`vehicleType`, `amount`)
                SELECT 'MOTORCYCLE', COALESCE(`monthlyBudgetMotorcycle`, 0)
                FROM `settings`
                WHERE `id` = 1
                """.trimIndent()
            )
        }
    }

    private fun recreateVehicles(database: SupportSQLiteDatabase) {
        val legacy = renameIfExists(database, "vehicles")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `vehicles` (
              `id` INTEGER NOT NULL,
              `name` TEXT NOT NULL,
              `type` TEXT NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        if (legacy != null) {
            val id = colOr(database, legacy, "id", "rowid")
            val name = colOr(database, legacy, "name", "''")
            val type = colOr(database, legacy, "type", "'CAR'")
            database.execSQL(
                """
                INSERT OR IGNORE INTO `vehicles` (`id`, `name`, `type`)
                SELECT $id, COALESCE($name, ''), COALESCE($type, 'CAR')
                FROM `$legacy`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `$legacy`")
        }
    }

    private fun recreateOdometerRecords(database: SupportSQLiteDatabase) {
        val legacy = renameIfExists(database, "odometer_records")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `odometer_records` (
              `id` INTEGER NOT NULL,
              `vehicleId` INTEGER NOT NULL,
              `dateEpochDay` INTEGER NOT NULL,
              `odometerKm` REAL NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (legacy != null) {
            val id = colOr(database, legacy, "id", "rowid")
            val vehicleId = colOr(database, legacy, "vehicleId", "1")
            val dateEpochDay = colOr(database, legacy, "dateEpochDay", "0")
            val odometerKm = colOr(database, legacy, "odometerKm", "0")
            database.execSQL(
                """
                INSERT OR IGNORE INTO `odometer_records` (`id`, `vehicleId`, `dateEpochDay`, `odometerKm`)
                SELECT $id, COALESCE($vehicleId, 1), COALESCE($dateEpochDay, 0), COALESCE($odometerKm, 0)
                FROM `$legacy`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `$legacy`")
        }

        database.execSQL("DROP INDEX IF EXISTS `index_odometer_records_vehicleId`")
        database.execSQL("DROP INDEX IF EXISTS `index_odometer_records_dateEpochDay`")
        database.execSQL("DROP INDEX IF EXISTS `index_odometer_records_vehicleId_dateEpochDay`")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_odometer_records_vehicleId` ON `odometer_records` (`vehicleId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_odometer_records_dateEpochDay` ON `odometer_records` (`dateEpochDay`)")
    }

    private fun recreateFuelRecords(database: SupportSQLiteDatabase) {
        val legacy = renameIfExists(database, "fuel_records")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fuel_records` (
              `id` INTEGER NOT NULL,
              `vehicleId` INTEGER NOT NULL,
              `dateEpochDay` INTEGER NOT NULL,
              `odometerKm` REAL NOT NULL,
              `liters` REAL NOT NULL,
              `pricePerLiter` REAL NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (legacy != null) {
            val id = colOr(database, legacy, "id", "rowid")
            val vehicleId = colOr(database, legacy, "vehicleId", "1")
            val dateEpochDay = colOr(database, legacy, "dateEpochDay", "0")
            val odometerKm = colOr(database, legacy, "odometerKm", "0")
            val liters = colOr(database, legacy, "liters", "0")
            val pricePerLiter = colOr(database, legacy, "pricePerLiter", "0")
            database.execSQL(
                """
                INSERT OR IGNORE INTO `fuel_records` (`id`, `vehicleId`, `dateEpochDay`, `odometerKm`, `liters`, `pricePerLiter`)
                SELECT $id, COALESCE($vehicleId, 1), COALESCE($dateEpochDay, 0), COALESCE($odometerKm, 0), COALESCE($liters, 0), COALESCE($pricePerLiter, 0)
                FROM `$legacy`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `$legacy`")
        }

        database.execSQL("DROP INDEX IF EXISTS `index_fuel_records_vehicleId`")
        database.execSQL("DROP INDEX IF EXISTS `index_fuel_records_dateEpochDay`")
        database.execSQL("DROP INDEX IF EXISTS `index_fuel_records_vehicleId_dateEpochDay`")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_vehicleId` ON `fuel_records` (`vehicleId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_dateEpochDay` ON `fuel_records` (`dateEpochDay`)")
    }

    private fun recreateMaintenanceRecords(database: SupportSQLiteDatabase) {
        val legacy = renameIfExists(database, "maintenance_records")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_records` (
              `id` INTEGER NOT NULL,
              `vehicleId` INTEGER NOT NULL,
              `type` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `notes` TEXT NOT NULL,
              `createdAtEpochDay` INTEGER NOT NULL,
              `dueDateEpochDay` INTEGER,
              `dueOdometerKm` REAL,
              `estimatedCost` REAL,
              `done` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (legacy != null) {
            val id = colOr(database, legacy, "id", "rowid")
            val vehicleId = colOr(database, legacy, "vehicleId", "1")
            val type = colOr(database, legacy, "type", "'OTHER'")
            val title = colOr(database, legacy, "title", "''")
            val notes = colOr(database, legacy, "notes", "''")
            val createdAtEpochDay = colOr(database, legacy, "createdAtEpochDay", "0")
            val dueDateEpochDay = nullableColOr(database, legacy, "dueDateEpochDay")
            val dueOdometerKm = nullableColOr(database, legacy, "dueOdometerKm")
            val estimatedCost = nullableColOr(database, legacy, "estimatedCost")
            val done = colOr(database, legacy, "done", "0")
            database.execSQL(
                """
                INSERT OR IGNORE INTO `maintenance_records` (
                  `id`, `vehicleId`, `type`, `title`, `notes`, `createdAtEpochDay`, `dueDateEpochDay`, `dueOdometerKm`, `estimatedCost`, `done`
                )
                SELECT
                  $id,
                  COALESCE($vehicleId, 1),
                  COALESCE($type, 'OTHER'),
                  COALESCE($title, ''),
                  COALESCE($notes, ''),
                  COALESCE($createdAtEpochDay, 0),
                  $dueDateEpochDay,
                  $dueOdometerKm,
                  $estimatedCost,
                  COALESCE($done, 0)
                FROM `$legacy`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `$legacy`")
        }

        database.execSQL("DROP INDEX IF EXISTS `index_maintenance_records_vehicleId`")
        database.execSQL("DROP INDEX IF EXISTS `index_maintenance_records_dueDateEpochDay`")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId` ON `maintenance_records` (`vehicleId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_dueDateEpochDay` ON `maintenance_records` (`dueDateEpochDay`)")
    }

    private fun recreateSettings(database: SupportSQLiteDatabase) {
        val legacy = renameIfExists(database, "settings")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `settings` (
              `id` INTEGER NOT NULL,
              `darkThemeEnabled` INTEGER NOT NULL,
              `legacyImportDone` INTEGER NOT NULL,
              `dataUpdatedAtMillis` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (legacy != null) {
            if (hasColumn(database, legacy, "key") && hasColumn(database, legacy, "longValue")) {
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO `settings` (`id`, `darkThemeEnabled`, `legacyImportDone`, `dataUpdatedAtMillis`)
                    VALUES (
                      1,
                      COALESCE((SELECT `longValue` FROM `$legacy` WHERE `key` = 'dark_theme' LIMIT 1), 0),
                      COALESCE((SELECT `longValue` FROM `$legacy` WHERE `key` = 'legacy_import_done' LIMIT 1), 0),
                      COALESCE((SELECT `longValue` FROM `$legacy` WHERE `key` = 'data_updated_at' LIMIT 1), 0)
                    )
                    """.trimIndent()
                )
            } else {
                val id = colOr(database, legacy, "id", "1")
                val darkThemeEnabled = colOr(database, legacy, "darkThemeEnabled", "0")
                val legacyImportDone = colOr(database, legacy, "legacyImportDone", "0")
                val dataUpdatedAtMillis = colOr(database, legacy, "dataUpdatedAtMillis", "0")
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO `settings` (`id`, `darkThemeEnabled`, `legacyImportDone`, `dataUpdatedAtMillis`)
                    SELECT
                      COALESCE($id, 1),
                      COALESCE($darkThemeEnabled, 0),
                      COALESCE($legacyImportDone, 0),
                      COALESCE($dataUpdatedAtMillis, 0)
                    FROM `$legacy`
                    LIMIT 1
                    """.trimIndent()
                )
            }
            database.execSQL("DROP TABLE `$legacy`")
        }

        database.execSQL(
            """
            INSERT OR IGNORE INTO `settings` (`id`, `darkThemeEnabled`, `legacyImportDone`, `dataUpdatedAtMillis`)
            VALUES (1, 0, 0, 0)
            """.trimIndent()
        )
    }

    private fun renameIfExists(database: SupportSQLiteDatabase, tableName: String): String? {
        if (!hasTable(database, tableName)) return null
        val legacy = "${tableName}_legacy_v1"
        if (hasTable(database, legacy)) {
            database.execSQL("DROP TABLE `$legacy`")
        }
        database.execSQL("ALTER TABLE `$tableName` RENAME TO `$legacy`")
        return legacy
    }

    private fun hasTable(database: SupportSQLiteDatabase, tableName: String): Boolean {
        database.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$tableName' LIMIT 1"
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun hasColumn(database: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex == -1) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
            return false
        }
    }

    private fun colOr(
        database: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        fallbackSql: String
    ): String {
        return if (hasColumn(database, tableName, columnName)) "`$columnName`" else fallbackSql
    }

    private fun nullableColOr(database: SupportSQLiteDatabase, tableName: String, columnName: String): String {
        return if (hasColumn(database, tableName, columnName)) "`$columnName`" else "NULL"
    }

    private fun addColumnIfMissing(
        database: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDefinition: String
    ) {
        if (hasColumn(database, tableName, columnName)) return
        database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
    }
}
