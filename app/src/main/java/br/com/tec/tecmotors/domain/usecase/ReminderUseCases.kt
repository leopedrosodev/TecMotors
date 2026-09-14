package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import java.time.LocalDate
import java.time.YearMonth

/**
 * Motivo concreto para incomodar o usuario. Se nao existe motivo, nao existe
 * notificacao - e isso que diferencia um lembrete util de um alarme diario
 * que o usuario aprende a ignorar.
 */
sealed interface ReminderAlert {
    val vehicleId: Long
    val vehicleName: String

    data class MaintenanceDueSoon(
        override val vehicleId: Long,
        override val vehicleName: String,
        val recordId: Long,
        val title: String,
        val kmRemaining: Double,
        val dueOdometerKm: Double,
        val currentOdometerKm: Double
    ) : ReminderAlert

    data class MonthEndWithoutOdometer(
        override val vehicleId: Long,
        override val vehicleName: String
    ) : ReminderAlert

    data class NoRecentRefuel(
        override val vehicleId: Long,
        override val vehicleName: String,
        val daysSinceLastRefuel: Int,
        val lastStationName: String
    ) : ReminderAlert
}

/**
 * Decide, a partir do estado local, quais lembretes fazem sentido hoje.
 *
 * Regra de ouro: veiculo sem nenhum historico nunca gera alerta - um usuario
 * que acabou de instalar o app nao e lembrado de algo que nunca fez.
 */
class DecideRemindersUseCase {

    operator fun invoke(
        today: LocalDate,
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
        refuelGapDays: Int = DEFAULT_REFUEL_GAP_DAYS,
        maintenanceThresholdKm: Double = DEFAULT_MAINTENANCE_THRESHOLD_KM,
        monthEndWindowDays: Int = DEFAULT_MONTH_END_WINDOW_DAYS
    ): List<ReminderAlert> {
        val knownVehicleIds = vehicles.map { it.id }.toSet()

        val vehicleFuels = fuelRecords
            .filter { it.vehicleId in knownVehicleIds }
            .groupBy { it.vehicleId }
        val vehicleOdometers = odometerRecords
            .filter { it.vehicleId in knownVehicleIds }
            .groupBy { it.vehicleId }

        val maintenanceAlerts = mutableListOf<ReminderAlert.MaintenanceDueSoon>()
        val monthEndAlerts = mutableListOf<ReminderAlert.MonthEndWithoutOdometer>()
        val refuelAlerts = mutableListOf<ReminderAlert.NoRecentRefuel>()

        vehicles.forEach { vehicle ->
            val fuels = vehicleFuels[vehicle.id].orEmpty()
            val odometers = vehicleOdometers[vehicle.id].orEmpty()
            if (fuels.isEmpty() && odometers.isEmpty()) return@forEach

            val currentKm = currentOdometerKm(fuels, odometers)

            if (currentKm != null) {
                maintenanceRecords
                    .asSequence()
                    .filter { it.vehicleId == vehicle.id && !it.done && it.dueOdometerKm != null }
                    .forEach { record ->
                        val dueKm = record.dueOdometerKm ?: return@forEach
                        val remaining = dueKm - currentKm
                        if (remaining > maintenanceThresholdKm) return@forEach

                        maintenanceAlerts += ReminderAlert.MaintenanceDueSoon(
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.name,
                            recordId = record.id,
                            title = record.title,
                            kmRemaining = remaining,
                            dueOdometerKm = dueKm,
                            currentOdometerKm = currentKm
                        )
                    }
            }

            if (isMonthEndWindow(today, monthEndWindowDays) &&
                !hasOdometerInMonth(odometers, YearMonth.from(today))
            ) {
                monthEndAlerts += ReminderAlert.MonthEndWithoutOdometer(
                    vehicleId = vehicle.id,
                    vehicleName = vehicle.name
                )
            }

            val lastRefuel = fuels.maxByOrNull { it.dateEpochDay }
            if (lastRefuel != null) {
                val days = (today.toEpochDay() - lastRefuel.dateEpochDay).toInt()
                if (days >= refuelGapDays) {
                    refuelAlerts += ReminderAlert.NoRecentRefuel(
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        daysSinceLastRefuel = days,
                        lastStationName = lastRefuel.stationName
                    )
                }
            }
        }

        return buildList {
            addAll(maintenanceAlerts.sortedBy { it.kmRemaining })
            addAll(monthEndAlerts)
            addAll(refuelAlerts.sortedByDescending { it.daysSinceLastRefuel })
        }
    }

    /**
     * Km atual = maior leitura conhecida, venha de odometro avulso ou do
     * odometro anotado num abastecimento. Km nao anda para tras.
     */
    private fun currentOdometerKm(
        fuels: List<FuelRecord>,
        odometers: List<OdometerRecord>
    ): Double? {
        val fromOdometers = odometers.maxOfOrNull { it.odometerKm }
        val fromFuels = fuels.maxOfOrNull { it.odometerKm }
        return listOfNotNull(fromOdometers, fromFuels).maxOrNull()
    }

    private fun isMonthEndWindow(today: LocalDate, windowDays: Int): Boolean {
        val remainingDays = today.lengthOfMonth() - today.dayOfMonth
        return remainingDays < windowDays
    }

    private fun hasOdometerInMonth(odometers: List<OdometerRecord>, month: YearMonth): Boolean {
        return odometers.any { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }
    }

    companion object {
        const val DEFAULT_REFUEL_GAP_DAYS = 14
        const val DEFAULT_MAINTENANCE_THRESHOLD_KM = 1_000.0
        const val DEFAULT_MONTH_END_WINDOW_DAYS = 3

        /** Janela maior na tela inicial: la o alerta informa, nao interrompe. */
        const val HOME_MAINTENANCE_THRESHOLD_KM = 2_000.0
    }
}
