package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.budget.CurrencyInputHelper.parseCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class ValueBudgetFragment : Fragment() {

    private lateinit var editTextBudget: EditText
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
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("$0.00")


        viewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            val formatted = currencyFormatter.format(BigDecimal(total.toString()))
            if (editTextBudget.text.toString() != formatted) {
                editTextBudget.setText(formatted)
                editTextBudget.setSelection(formatted.length)
            }
        }


        viewModel.categoryBudgets.observe(viewLifecycleOwner) { categoryMap ->
            categoryMap.forEach { (category, amount) ->
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val formatted = currencyFormatter.format(BigDecimal(amount.toString()))
                if (editText.text.toString() != formatted) {
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }
            }
        }

        setupCategoryInputs(view, viewModel)


        editTextBudget.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isCurrencyFormatting) return
                isCurrencyFormatting = true

                val parsedValue = parseCurrency(s.toString()).toFloat()
                viewModel.setTotalBudget(parsedValue)

                val formatted = currencyFormatter.format(BigDecimal(parsedValue.toString()))
                if (editTextBudget.text.toString() != formatted) {
                    editTextBudget.setText(formatted)
                    editTextBudget.setSelection(formatted.length)
                }

                isCurrencyFormatting = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupCategoryInputs(view: View, viewModel: BudgetViewModel) {
        val categoryFields: Map<CategoryType, EditText> = mapOf(
            CategoryType.FOOD to view.findViewById(R.id.et_food),
            CategoryType.LIVING to view.findViewById(R.id.et_home),
            CategoryType.HEALTH to view.findViewById(R.id.et_health),
            CategoryType.RECREATION to view.findViewById(R.id.et_recreation),
            CategoryType.TRANSPORT to view.findViewById(R.id.et_transport),
            CategoryType.OTHER to view.findViewById(R.id.et_others)
        )

        categoryFields.forEach { (category, editText) ->
            editText.setText("$0.00")

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isCurrencyFormatting) return
                    isCurrencyFormatting = true

                    val newValue = parseCurrency(s.toString()).toFloat()
                    val totalBudget = parseCurrency(editTextBudget.text.toString()).toFloat()

                    val sumOfOthers = categoryFields
                        .filter { it.key != category }
                        .map { parseCurrency(it.value.text.toString()).toFloat() }
                        .sum()

                    val maxAllowed = totalBudget - sumOfOthers
                    val finalValue = newValue.coerceAtMost(maxAllowed)

                    // Guarda en el ViewModel
                    viewModel.setCategoryBudget(category.name, finalValue)

                    // Formatea si hubo ajuste
                    val formatted = currencyFormatter.format(BigDecimal(finalValue.toString()))
                    if (editText.text.toString() != formatted) {
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    }

                    isCurrencyFormatting = false
                }
            })
        }
    }

    private fun parseCurrency(input: String): BigDecimal {
        return try {

            BigDecimal(input.replace("[^\\d]".toRegex(), "")).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }


    private fun getEditTextIdForCategory(category: CategoryType): Int {
        return when (category) {
            CategoryType.FOOD -> R.id.et_food
            CategoryType.LIVING -> R.id.et_home
            CategoryType.HEALTH -> R.id.et_health
            CategoryType.RECREATION -> R.id.et_recreation
            CategoryType.TRANSPORT -> R.id.et_transport
            CategoryType.OTHER -> R.id.et_others
        }
    }

}
