package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale


class PercentageBudgetFragment : Fragment() {
    private lateinit var editTextBudget: EditText
    private val currencyFormatter = DecimalFormat("$#,##0.00").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumIntegerDigits = 7
        minimumIntegerDigits = 1
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    private var isCurrencyFormatting = false

    private lateinit var editTextAmountFood: EditText
    private lateinit var editTextAmountHome: EditText
    private lateinit var editTextAmountRecreation: EditText
    private lateinit var editTextAmountHealth: EditText
    private lateinit var editTextAmountTransport: EditText
    private lateinit var editTextAmountOthers: EditText
    private val percentageFields = mutableListOf<EditText>()
    private var isPercentageChange = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_percentage_budget, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextAmountFood = view.findViewById(R.id.et_food)
        editTextAmountHome = view.findViewById(R.id.et_home)
        editTextAmountHealth = view.findViewById(R.id.et_health)
        editTextAmountRecreation = view.findViewById(R.id.et_recreation)
        editTextAmountTransport = view.findViewById(R.id.et_transport)
        editTextAmountOthers = view.findViewById(R.id.et_others)

        percentageFields.addAll(listOf(
            editTextAmountFood,
            editTextAmountHome,
            editTextAmountHealth,
            editTextAmountRecreation,
            editTextAmountTransport,
            editTextAmountOthers))

        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("$0.00")

        setupPercentageListeners()
        setupCurrencyListener()
    }

    private fun setupPercentageListeners() {
        percentageFields.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    if (!isPercentageChange) handlePercentageChange(editText, editable)
                }
            })
        }
    }

    private fun setupCurrencyListener() {
        editTextBudget.addTextChangedListener(object : TextWatcher {
            private var lastValidInput = ""
            private var currentCursorPos = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                currentCursorPos = editTextBudget.selectionStart
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isCurrencyFormatting) return
                isCurrencyFormatting = true

                val original = editable.toString()
                val (cleanInput, newCursorPos) = processInput(original, currentCursorPos)

                try {
                    val parsed = BigDecimal(cleanInput).movePointLeft(2)
                    val validated = parsed.coerceIn(BigDecimal.ZERO, BigDecimal("9999999.99"))

                    val formatted = currencyFormatter.format(validated)
                    if (formatted != original) {
                        editTextBudget.setText(formatted)
                        val selection = calculateCursorPosition(formatted, newCursorPos)
                        editTextBudget.setSelection(selection.coerceIn(1, formatted.length))
                    }
                    lastValidInput = formatted
                } catch (e: Exception) {
                    editTextBudget.setText(lastValidInput)
                    editTextBudget.setSelection(lastValidInput.length)
                }

                isCurrencyFormatting = false
            }
        })
    }

    private fun processInput(input: String, cursorPos: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var newCursorPos = cursorPos
        var decimalFound = false
        var digitsAfterDecimal = 0

        // Track decimal point and digits
        input.forEachIndexed { index, c ->
            when {
                c == '.' && !decimalFound -> {
                    decimalFound = true
                    if (index <= cursorPos) newCursorPos--
                }
                c.isDigit() -> {
                    if (decimalFound) digitsAfterDecimal++
                    if (digitsAfterDecimal <= 2) sb.append(c)
                    else if (index < cursorPos) newCursorPos--
                }
                else -> if (index < cursorPos) newCursorPos--
            }
        }

        val cleanInput = when {
            decimalFound -> sb.toString().padEnd(sb.indexOf('.') + 3, '0')
            else -> sb.append("00").toString()
        }

        return cleanInput to newCursorPos
    }

    private fun calculateCursorPosition(formatted: String, originalCursor: Int): Int {
        val unformatted = formatted.replace("[^\\d]".toRegex(), "")
        var currentPos = 0
        var formattedPos = 0

        while (currentPos < originalCursor && formattedPos < formatted.length) {
            if (formatted[formattedPos].isDigit()) currentPos++
            formattedPos++
        }

        return formattedPos
    }

    private fun handlePercentageChange(currentEditText: EditText, editable: Editable?) {
        isPercentageChange = true

        val input = editable?.toString()?.replace(Regex("[^\\d]"), "") ?: ""
        var value = input.toIntOrNull() ?: 0

        // Individual maximum
        value = value.coerceAtMost(100)

        // Calculate sum of other fields
        val sumOthers = percentageFields
            .filter { it != currentEditText }
            .sumOf {
                it.text.toString().replace(Regex("[^\\d]"), "").toIntOrNull() ?: 0
            }

        // Total maximum
        val maxAllowed = (100 - sumOthers).coerceAtLeast(0)
        value = value.coerceAtMost(maxAllowed)

        // Update text
        val newText = "$value%"
        currentEditText.setText(newText)
        currentEditText.setSelection(newText.length - 1)

        isPercentageChange = false
    }



    private fun selectionPosition(original: String, formatted: String): Int {
        val commaCountOriginal = original.count { it == ',' }
        val commaCountFormatted = formatted.count { it == ',' }
        return formatted.length - (original.length - editTextBudget.selectionStart) +
                (commaCountFormatted - commaCountOriginal)
    }


}