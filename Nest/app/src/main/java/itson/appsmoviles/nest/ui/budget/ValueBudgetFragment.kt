package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class ValueBudgetFragment : Fragment() {

    private lateinit var editTextBudget: EditText
    private lateinit var editTextAmountFood: EditText
    private lateinit var editTextAmountHome: EditText
    private lateinit var editTextAmountRecreation: EditText
    private lateinit var editTextAmountHealth: EditText
    private lateinit var editTextAmountTransport: EditText
    private lateinit var editTextAmountOthers: EditText
    private val currencyFields = mutableListOf<EditText>()
    private val currencyFormatter = DecimalFormat("$#,##0.00").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumIntegerDigits = 7
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    private var isCurrencyFormatting = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_budget, container, false)


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

        currencyFields.addAll(listOf(
            editTextAmountFood,
            editTextAmountHome,
            editTextAmountHealth,
            editTextAmountRecreation,
            editTextAmountTransport,
            editTextAmountOthers))

        editTextBudget = view.findViewById(R.id.monthly_budget)

        listOf(editTextBudget).plus(currencyFields).forEach {
            it.setText("$0.00")
        }

        setupCurrencyFormatting()

    }

    private fun setupCurrencyFormatting() {
        editTextBudget.addTextChangedListener(createMainTextWatcher())
        currencyFields.forEach { editText ->
            editText.addTextChangedListener(createAdditionalTextWatcher(editText))
        }
    }

    private fun createMainTextWatcher(): TextWatcher {
        return object : TextWatcher {
            private var previousMainValue = BigDecimal.ZERO

            override fun beforeTextChanged(s: CharSequence?, st: Int, co: Int, af: Int) {
                previousMainValue = parseCurrency(s.toString())
            }

            override fun onTextChanged(s: CharSequence?, st: Int, be: Int, co: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isCurrencyFormatting) return
                isCurrencyFormatting = true

                val (formatted, parsed) = processCurrencyInput(editable.toString())
                val currentSum = calculateAdditionalSum()

                when {
                    parsed < currentSum -> {
                        editTextBudget.setText(currencyFormatter.format(previousMainValue))
                        editTextBudget.setSelection(editTextBudget.text?.length ?: 0)
                    }
                    formatted != editable.toString() -> {
                        editTextBudget.setText(formatted)
                        editTextBudget.setSelection(formatted.length)
                    }
                }
                
                currencyFields.forEach { field ->
                    if (field.text.toString() != "0.00") {
                        field.setText(field.text.toString())
                    }
                }

                isCurrencyFormatting = false
            }
        }
    }

    private fun createAdditionalTextWatcher(currentField: EditText): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isCurrencyFormatting) return
                isCurrencyFormatting = true

                val mainValue = parseCurrency(editTextBudget.text.toString())

                val sumOthers = currencyFields
                    .filter { it != currentField }
                    .sumOf { parseCurrency(it.text.toString()) }

                val maxAllowed = (mainValue - sumOthers).coerceAtLeast(BigDecimal.ZERO)

                val (formatted, parsed) = processCurrencyInput(editable.toString())
                val adjustedValue = parsed.coerceAtMost(maxAllowed)

                if (adjustedValue != parsed || formatted != editable.toString()) {
                    val finalText = currencyFormatter.format(adjustedValue)
                    currentField.setText(finalText)
                    currentField.setSelection(finalText.length)
                }

                isCurrencyFormatting = false
            }
        }
    }

    private fun processCurrencyInput(input: String): Pair<String, BigDecimal> {
        val clean = input.replace("[^\\d]".toRegex(), "")
        val parsed = try {
            BigDecimal(clean).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
        return currencyFormatter.format(parsed) to parsed
    }

    private fun parseCurrency(input: String): BigDecimal {
        return try {
            BigDecimal(input.replace("[^\\d]".toRegex(), "")).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun calculateAdditionalSum(): BigDecimal {
        return currencyFields.fold(BigDecimal.ZERO) { acc, et ->
            acc + parseCurrency(et.text.toString())
        }
    }

}