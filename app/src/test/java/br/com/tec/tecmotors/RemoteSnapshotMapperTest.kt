package br.com.tec.tecmotors

import br.com.tec.tecmotors.data.remote.RemoteSnapshotMapper
import br.com.tec.tecmotors.domain.model.FuelUsageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSnapshotMapperTest {
    @Test
    fun fromRemoteMap_handlesOldSchemaWithoutMaintenance() {
        val remote = mapOf<String, Any?>(
            "schemaVersion" to 1L,
            "updatedAtMillis" to 1000L,
            "vehicles" to listOf(
                mapOf("id" to 1L, "name" to "Carro", "type" to "CAR")
            ),
            "odometerRecords" to listOf(
                mapOf("id" to 2L, "vehicleId" to 1L, "dateEpochDay" to 100L, "odometerKm" to 1234.0)
            ),
            "fuelRecords" to listOf(
                mapOf(
                    "id" to 3L,
                    "vehicleId" to 1L,
                    "dateEpochDay" to 101L,
                    "odometerKm" to 1300.0,
                    "liters" to 20.0,
                    "pricePerLiter" to 5.0
                )
            )
        )

        val snapshot = RemoteSnapshotMapper.fromRemoteMap(remote)

        assertEquals(1, snapshot.vehicles.size)
        assertEquals(1, snapshot.odometerRecords.size)
        assertEquals(1, snapshot.fuelRecords.size)
        assertTrue(snapshot.maintenanceRecords.isEmpty())
        assertEquals(FuelUsageType.MIXED, snapshot.fuelRecords.first().usageType)
        assertEquals(1000L, snapshot.updatedAtMillis)
    }
}
