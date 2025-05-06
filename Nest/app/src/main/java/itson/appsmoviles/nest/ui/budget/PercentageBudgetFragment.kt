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
        setupCurrencyFormatting()
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

    private fun setupCurrencyFormatting() {
        editTextBudget.addTextChangedListener(createMainTextWatcher())
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

                when {
                    formatted != editable.toString() -> {
                        editTextBudget.setText(formatted)
                        editTextBudget.setSelection(formatted.length)
                    }
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


    private fun handlePercentageChange(currentEditText: EditText, editable: Editable?) {
        isPercentageChange = true

        val input = editable?.toString()?.replace(Regex("[^\\d]"), "") ?: ""
        var value = input.toIntOrNull() ?: 0

        value = value.coerceAtMost(100)

        val sumOthers = percentageFields
            .filter { it != currentEditText }
            .sumOf {
                it.text.toString().replace(Regex("[^\\d]"), "").toIntOrNull() ?: 0
            }

        val maxAllowed = (100 - sumOthers).coerceAtLeast(0)
        value = value.coerceAtMost(maxAllowed)

        val newText = "$value%"
        currentEditText.setText(newText)
        currentEditText.setSelection(newText.length - 1)

        isPercentageChange = false
    }

}