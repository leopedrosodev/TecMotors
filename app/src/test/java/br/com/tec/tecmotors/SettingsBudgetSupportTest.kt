package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.model.monthlyBudgetFor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBudgetSupportTest {
    private val settings = Settings(
        darkThemeEnabled = true,
        legacyImportDone = false,
        dataUpdatedAtMillis = 0L,
        monthlyBudgetCar = 450.0,
        monthlyBudgetMotorcycle = 180.0
    )

    private val mappedSettings = settings.copy(
        vehicleBudgets = mapOf(
            VehicleType.CAR to 470.0,
            VehicleType.MOTORCYCLE to 190.0,
            VehicleType.OTHER to 250.0
        )
    )

    @Test
    fun monthlyBudgetFor_returnsBudgetForDedicatedTypes() {
        assertEquals(450.0, settings.monthlyBudgetFor(VehicleType.CAR), 0.001)
        assertEquals(180.0, settings.monthlyBudgetFor(VehicleType.MOTORCYCLE), 0.001)
    }

    @Test
    fun monthlyBudgetFor_returnsZeroForOtherAndNull() {
        assertEquals(0.0, settings.monthlyBudgetFor(VehicleType.OTHER), 0.001)
        assertEquals(0.0, settings.monthlyBudgetFor(null), 0.001)
    }

    @Test
    fun monthlyBudgetFor_prefersMappedBudgetsWhenAvailable() {
        assertEquals(470.0, mappedSettings.monthlyBudgetFor(VehicleType.CAR), 0.001)
        assertEquals(190.0, mappedSettings.monthlyBudgetFor(VehicleType.MOTORCYCLE), 0.001)
        assertEquals(250.0, mappedSettings.monthlyBudgetFor(VehicleType.OTHER), 0.001)
    }
}
