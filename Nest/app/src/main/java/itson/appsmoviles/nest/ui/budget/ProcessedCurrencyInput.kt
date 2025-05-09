package itson.appsmoviles.nest.ui.budget

import java.math.BigDecimal

data class ProcessedCurrencyInput(val displayString: String, val parsedValue: BigDecimal, val rawCleanedNumberString: String)
