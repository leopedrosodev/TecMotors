package br.com.tec.tecmotors

import br.com.tec.tecmotors.data.CsvExporter
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun buildSnapshotCsv_containsAllSections_andEscapesText() {
        val snapshot = LocalStateSnapshot(
            vehicles = listOf(Vehicle(1, "Meu \"Carro\"", VehicleType.CAR)),
            odometerRecords = listOf(OdometerRecord(2, 1, 100L, 1234.5)),
            fuelRecords = listOf(
                FuelRecord(
                    id = 3,
                    vehicleId = 1,
                    dateEpochDay = 101L,
                    odometerKm = 1300.0,
                    liters = 20.0,
                    pricePerLiter = 5.0,
                    stationName = "Posto Azul",
                    usageType = FuelUsageType.CITY,
                    receiptImageUri = "content://receipt/1"
                )
            ),
            maintenanceRecords = listOf(
                MaintenanceRecord(
                    id = 4,
                    vehicleId = 1,
                    type = MaintenanceType.OTHER,
                    title = "Filtro",
                    notes = "texto;com;separador",
                    createdAtEpochDay = 102L,
                    dueDateEpochDay = null,
                    dueOdometerKm = null,
                    estimatedCost = null,
                    done = false,
                    receiptImageUri = "content://receipt/2"
                )
            ),
            updatedAtMillis = 1L
        )

        val csv = CsvExporter.buildSnapshotCsv(snapshot)

        assertTrue(csv.contains("[VEICULOS]"))
        assertTrue(csv.contains("[ODOMETRO]"))
        assertTrue(csv.contains("[ABASTECIMENTOS]"))
        assertTrue(csv.contains("[MANUTENCAO]"))
        assertTrue(csv.contains("\"Meu \"\"Carro\"\"\""))
        assertTrue(csv.contains("\"texto;com;separador\""))
        assertTrue(csv.contains("\"Posto Azul\""))
        assertTrue(csv.contains("\"CITY\""))
        assertTrue(csv.contains("\"content://receipt/2\""))
    }
}
