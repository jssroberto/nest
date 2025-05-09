package itson.appsmoviles.nest.ui.budget

import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import java.math.BigDecimal
import java.text.DecimalFormat

fun processAndFormatCurrencyInput(
    userInput: String,
    formatter: DecimalFormat,
    previousParsedValueForFallback: BigDecimal = BigDecimal.ZERO // Optional: for fallback like in totalBudgetWatcher
): ProcessedCurrencyInput {
    val localDecimalSeparator = formatter.decimalFormatSymbols.decimalSeparator.toString()
    val localGroupingSeparator = formatter.decimalFormatSymbols.groupingSeparator.toString()

    // 1. Clean the input string (copied from totalBudgetWatcher)
    var cleanString = userInput.replace("$", "")
    // It's safer to remove grouping separators that are NOT the decimal separator first.
    // However, the original totalBudgetWatcher logic handles this by specific replacements.
    // We'll stick to its tested logic for cleaning based on identified separators.
    cleanString = cleanString.replace(localGroupingSeparator, "") // Remove grouping separator

    // Standardize decimal separator to '.' for BigDecimal
    if (localDecimalSeparator == ",") {
        // If user typed "1.234,56", cleanString is "1.234,56" after removing groupSep (if groupSep != '.')
        // If groupSep was '.', cleanString is "1234,56"
        // This part ensures that '.' are removed if they are not the decimal sep, and ',' is turned to '.' if it is.
        cleanString = cleanString.replace(".", "") // Remove thousand separators if they were '.'
        cleanString = cleanString.replaceFirst(',', '.') // Convert decimal comma to dot
    } else { // localDecimalSeparator is likely "."
        // If user typed "1,234.56", cleanString is "1,234.56" (if groupSep != ',')
        // This removes thousand separators if they were ','
        cleanString = cleanString.replace(",", "") // Remove thousand separators if they were ','
    }


    // Ensure only one actual decimal point for parsing & limit fraction digits
    val firstDecimalIndex = cleanString.indexOf('.')
    if (firstDecimalIndex != -1) {
        val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
        var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")
        if (afterDecimal.length > formatter.maximumFractionDigits) {
            afterDecimal = afterDecimal.substring(0, formatter.maximumFractionDigits)
        }
        cleanString = beforeDecimal + afterDecimal
    }

    val numberToParse = when {
        cleanString == "." -> "0."
        cleanString.isEmpty() -> "0"
        else -> cleanString
    }

    // 2. Parse the cleaned string
    val parsedBigDecimal = try {
        BigDecimal(numberToParse)
    } catch (e: NumberFormatException) {
        previousParsedValueForFallback // Use provided fallback
    }

    // 4. Format for display
    var formattedDisplayString = "$" + formatter.format(parsedBigDecimal)

    // 5. Preserve user-typed decimal point if necessary
    val userTypedDecimalAtEnd = (userInput.endsWith(localDecimalSeparator) || (userInput.endsWith(".") && localDecimalSeparator != "."))
    val formattedStringLostDecimal = !formattedDisplayString.contains(localDecimalSeparator) && (localDecimalSeparator == "." && !formattedDisplayString.contains(".")) || (localDecimalSeparator == "," && !formattedDisplayString.contains(","))

    val isWholeNumberBasically = parsedBigDecimal.stripTrailingZeros().scale() <= 0


    if (userTypedDecimalAtEnd && formattedStringLostDecimal && isWholeNumberBasically) {
        // If cleanString ended with a decimal point and it's a whole number, append it
        if (numberToParse.endsWith(".")) { // check numberToParse as it's the direct precursor to BigDecimal
            formattedDisplayString += localDecimalSeparator
        }
    } else if (numberToParse == "0." && (formattedDisplayString == "$0" || formattedDisplayString == "$0${localGroupingSeparator}00")) { // Handle "0." specifically. Formatter might add .00 then strip.
        formattedDisplayString = "$0$localDecimalSeparator"
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
