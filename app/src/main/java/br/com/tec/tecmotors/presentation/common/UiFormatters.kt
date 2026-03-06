package br.com.tec.tecmotors.presentation.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBrLocale = Locale.forLanguageTag("pt-BR")

val dateBrFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", ptBrLocale)

private val acceptedDateFormatters: List<DateTimeFormatter> = listOf(
    dateBrFormatter,
    DateTimeFormatter.ofPattern("d/M/uuuu", ptBrLocale),
    DateTimeFormatter.ofPattern("dd-MM-uuuu", ptBrLocale),
    DateTimeFormatter.ofPattern("d-M-uuuu", ptBrLocale),
    DateTimeFormatter.ISO_LOCAL_DATE
)

fun todayBr(): String = LocalDate.now().format(dateBrFormatter)

fun parseDateBrOrIso(input: String): LocalDate? {
    val value = input.trim()
    if (value.isBlank()) return null

    return acceptedDateFormatters.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDate.parse(value, formatter) }.getOrNull()
    }
}

fun parseDecimal(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    return normalized.toDoubleOrNull()
}

fun formatDate(epochDay: Long): String {
    return runCatching {
        LocalDate.ofEpochDay(epochDay).format(dateBrFormatter)
    }.getOrDefault("-")
}

fun formatNumber(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(ptBrLocale)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(value)
}

fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(ptBrLocale).format(value)
}
