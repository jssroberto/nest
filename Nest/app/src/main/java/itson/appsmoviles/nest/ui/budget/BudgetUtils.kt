package itson.appsmoviles.nest.ui.budget

import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

fun processAndFormatCurrencyInput(
    userInput: String,
    formatter: DecimalFormat,
    previousParsedValueForFallback: BigDecimal = BigDecimal.ZERO
): ProcessedCurrencyInput {
    val localDecimalSeparator = formatter.decimalFormatSymbols.decimalSeparator.toString()
    val localGroupingSeparator = formatter.decimalFormatSymbols.groupingSeparator.toString()

    var cleanString = userInput.replace(formatter.currency.symbol, "") // Remove currency symbol
    cleanString = cleanString.replace(localGroupingSeparator, "") // Remove grouping separator

    // Standardize decimal separator to '.' for BigDecimal parsing
    if (localDecimalSeparator != ".") {
        cleanString = cleanString.replace(localDecimalSeparator, ".")
    }

    // Handle potential multiple decimal points - keep only the last one
    val firstDecimalIndex = cleanString.indexOf('.')
    if (firstDecimalIndex != -1) {
        val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
        var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")
        // Limit fraction digits if needed, though BigDecimal parsing handles this
        cleanString = beforeDecimal + afterDecimal
    }


    val numberToParse = when {
        cleanString == "." -> "0." // Handle case where user types just a decimal point
        cleanString.isEmpty() -> "0" // Handle empty string as 0
        else -> cleanString
    }

    val parsedBigDecimal = try {
        BigDecimal(numberToParse).setScale(formatter.maximumFractionDigits, RoundingMode.DOWN)
    } catch (e: NumberFormatException) {
        previousParsedValueForFallback // Fallback on parse error
    }

    // Format for display using the formatter's locale rules
    var formattedDisplayString = formatter.format(parsedBigDecimal)

    // Preserve user-typed decimal point if necessary for a more natural input feel
    val userTypedDecimalAtEnd = userInput.endsWith(localDecimalSeparator)
    val formattedStringLostDecimal =
        !formattedDisplayString.contains(localDecimalSeparator) && (localDecimalSeparator == "." && !formattedDisplayString.contains(
            "."
        )) || (localDecimalSeparator == "," && !formattedDisplayString.contains(","))

    val isWholeNumberBasically = parsedBigDecimal.stripTrailingZeros().scale() <= 0


    if (userTypedDecimalAtEnd && formattedStringLostDecimal && isWholeNumberBasically) {
        // If the clean string ended with a decimal point and the formatted string didn't keep it, add it back
        if (numberToParse.endsWith(".")) {
            formattedDisplayString += localDecimalSeparator
        }
    } else if (numberToParse == "0." && !formattedDisplayString.contains(localDecimalSeparator)) {
        // Specific case for "0." input if formatter formats it as "$0" or similar without decimal
        formattedDisplayString = formatter.currency.symbol + "0" + localDecimalSeparator
    }


    return ProcessedCurrencyInput(formattedDisplayString, parsedBigDecimal, numberToParse)
}

fun getEditTextIdForCategory(category: CategoryType) = when (category) {
    CategoryType.FOOD -> R.id.et_food
    CategoryType.LIVING -> R.id.et_home
    CategoryType.HEALTH -> R.id.et_health
    CategoryType.RECREATION -> R.id.et_recreation
    CategoryType.TRANSPORT -> R.id.et_transport
    CategoryType.OTHER -> R.id.et_others
}

fun getEditTextIdForCategoryThreshold(category: CategoryType) = when (category) {
    CategoryType.FOOD -> R.id.editTextAlarmFood
    CategoryType.LIVING -> R.id.editTextAlarmHome
    CategoryType.HEALTH -> R.id.editTextAlarmHealth
    CategoryType.RECREATION -> R.id.editTextAlarmRecreation
    CategoryType.TRANSPORT -> R.id.editTextAlarmTransport
    CategoryType.OTHER -> R.id.editTextAlarmOthers
}

fun getSwitchIdForCategory(category: CategoryType) = when (category) {
    CategoryType.FOOD -> R.id.checkBoxAlarmFood
    CategoryType.LIVING -> R.id.checkBoxAlarmHome
    CategoryType.HEALTH -> R.id.checkBoxAlarmHealth
    CategoryType.RECREATION -> R.id.checkBoxAlarmRecreation
    CategoryType.TRANSPORT -> R.id.checkBoxAlarmTransport
    CategoryType.OTHER -> R.id.checkBoxAlarmOthers
}
