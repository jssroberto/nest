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

    var cleanString = userInput.replace(formatter.currency.symbol, "")
    cleanString = cleanString.replace(localGroupingSeparator, "")


    if (localDecimalSeparator != ".") {
        cleanString = cleanString.replace(localDecimalSeparator, ".")
    }


    val firstDecimalIndex = cleanString.indexOf('.')
    if (firstDecimalIndex != -1) {
        val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
        var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")

        cleanString = beforeDecimal + afterDecimal
    }


    val numberToParse = when {
        cleanString == "." -> "0."
        cleanString.isEmpty() -> "0"
        else -> cleanString
    }

    val parsedBigDecimal = try {
        BigDecimal(numberToParse).setScale(formatter.maximumFractionDigits, RoundingMode.DOWN)
    } catch (e: NumberFormatException) {
        previousParsedValueForFallback
    }


    var formattedDisplayString = formatter.format(parsedBigDecimal)


    val userTypedDecimalAtEnd = userInput.endsWith(localDecimalSeparator)
    val formattedStringLostDecimal =
        !formattedDisplayString.contains(localDecimalSeparator) && (localDecimalSeparator == "." && !formattedDisplayString.contains(
            "."
        )) || (localDecimalSeparator == "," && !formattedDisplayString.contains(","))

    val isWholeNumberBasically = parsedBigDecimal.stripTrailingZeros().scale() <= 0


    if (userTypedDecimalAtEnd && formattedStringLostDecimal && isWholeNumberBasically) {

        if (numberToParse.endsWith(".")) {
            formattedDisplayString += localDecimalSeparator
        }
    } else if (numberToParse == "0." && !formattedDisplayString.contains(localDecimalSeparator)) {

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
