package itson.appsmoviles.nest.ui.budget

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

// utils/CurrencyInputHelper.kt
object CurrencyInputHelper {
    private val formatter = DecimalFormat("$#,##0.00").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumIntegerDigits = 7
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun parseCurrency(input: String): BigDecimal {
        return try {
            BigDecimal(input.replace("[^\\d]".toRegex(), "")).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun formatCurrency(value: BigDecimal): String {
        return formatter.format(value)
    }

    fun processInput(input: String): Pair<String, BigDecimal> {
        val parsed = parseCurrency(input)
        return formatCurrency(parsed) to parsed
    }
}
