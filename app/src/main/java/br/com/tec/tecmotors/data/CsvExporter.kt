package br.com.tec.tecmotors.data

import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExporter {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))

    fun buildSnapshotCsv(snapshot: LocalStateSnapshot): String {
        val lines = mutableListOf<String>()

        lines += "tipo;arquivo;gerado_em"
        lines += rowOf(
            "metadata",
            "tec_motors_export",
            Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .toString()
        )
        lines += ""

        lines += "[VEICULOS]"
        lines += "id;nome;tipo"
        snapshot.vehicles
            .sortedBy { it.id }
            .forEach { vehicle ->
                lines += rowOf(vehicle.id, vehicle.name, vehicle.type.name)
            }
        lines += ""

        lines += "[ODOMETRO]"
        lines += "id;veiculo_id;data;odometro_km"
        snapshot.odometerRecords
            .sortedBy { it.id }
            .forEach { record ->
                lines += rowOf(
                    record.id,
                    record.vehicleId,
                    formatEpochDay(record.dateEpochDay),
                    formatDecimal(record.odometerKm)
                )
            }
        lines += ""

        lines += "[ABASTECIMENTOS]"
        lines += "id;veiculo_id;data;odometro_km;litros;preco_por_litro;posto;uso;comprovante_uri;total"
        snapshot.fuelRecords
            .sortedBy { it.id }
            .forEach { record ->
                lines += rowOf(
                    record.id,
                    record.vehicleId,
                    formatEpochDay(record.dateEpochDay),
                    formatDecimal(record.odometerKm),
                    formatDecimal(record.liters),
                    formatDecimal(record.pricePerLiter),
                    record.stationName,
                    record.usageType.name,
                    record.receiptImageUri.orEmpty(),
                    formatDecimal(record.totalCost)
                )
            }
        lines += ""

        lines += "[MANUTENCAO]"
        lines += "id;veiculo_id;tipo;titulo;observacoes;data_criacao;vencimento_data;vencimento_odometro_km;custo_estimado;concluida;comprovante_uri"
        snapshot.maintenanceRecords
            .sortedBy { it.id }
            .forEach { record ->
                lines += rowOf(
                    record.id,
                    record.vehicleId,
                    record.type.name,
                    record.title,
                    record.notes,
                    formatEpochDay(record.createdAtEpochDay),
                    record.dueDateEpochDay?.let(::formatEpochDay).orEmpty(),
                    record.dueOdometerKm?.let(::formatDecimal).orEmpty(),
                    record.estimatedCost?.let(::formatDecimal).orEmpty(),
                    if (record.done) "sim" else "nao",
                    record.receiptImageUri.orEmpty()
                )
            }

        return lines.joinToString("\n")
    }

    private fun rowOf(vararg values: Any?): String {
        return values.joinToString(";") { value -> escape(value?.toString().orEmpty()) }
    }

    private fun escape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatDecimal(value: Double): String {
        return "%.2f".format(Locale.US, value)
    }

    private fun formatEpochDay(epochDay: Long): String {
        return runCatching {
            java.time.LocalDate.ofEpochDay(epochDay).format(dateFormatter)
        }.getOrDefault("")
    }
}
