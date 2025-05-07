package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale


class PercentageBudgetFragment : Fragment() {
    private lateinit var viewModel: BudgetViewModel

    private lateinit var editTextBudget: EditText

    private val percentageFields = mutableMapOf<CategoryType, EditText>()
    private var isEditing = false

    private val currencyFormatter = DecimalFormat("$#,##0.00").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_percentage_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        editTextBudget = view.findViewById(R.id.monthly_budget)

        percentageFields[CategoryType.FOOD] = view.findViewById(R.id.et_food)
        percentageFields[CategoryType.LIVING] = view.findViewById(R.id.et_home)
        percentageFields[CategoryType.HEALTH] = view.findViewById(R.id.et_health)
        percentageFields[CategoryType.RECREATION] = view.findViewById(R.id.et_recreation)
        percentageFields[CategoryType.TRANSPORT] = view.findViewById(R.id.et_transport)
        percentageFields[CategoryType.OTHER] = view.findViewById(R.id.et_others)

        setupObservers()
        setupPercentageListeners()
    }

    private fun setupObservers() {
        viewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            editTextBudget.setText(currencyFormatter.format(total))
            updatePercentageFields()
        }

        viewModel.categoryBudgets.observe(viewLifecycleOwner) {
            updatePercentageFields()
        }
    }

    private fun updatePercentageFields() {
        val total = viewModel.totalBudget.value ?: return
        val categories = viewModel.categoryBudgets.value ?: return

        isEditing = true
        percentageFields.forEach { (category, field) ->
            val amount = categories[category] ?: 0f
            val percentage = if (total > 0f) (amount / total * 100).toInt() else 0
            val currentText = field.text.toString().replace("%", "")
            if (currentText != percentage.toString()) {
                field.setText("$percentage%")
                field.setSelection(field.text.length - 1)
            }
        }
        isEditing = false
    }

    private fun setupPercentageListeners() {
        percentageFields.forEach { (category, field) ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isEditing) return
                    val rawInput = s?.toString()?.replace(Regex("[^\\d]"), "") ?: return

                    var percent = rawInput.toIntOrNull() ?: return
                    percent = percent.coerceIn(0, 100)

                    val total = viewModel.totalBudget.value ?: return
                    val otherSum = percentageFields
                        .filter { it.key != category }
                        .mapNotNull {
                            it.value.text.toString()
                                .replace(Regex("[^\\d]"), "")
                                .toIntOrNull()
                        }
                        .sum()

                    val maxAllowed = (100 - otherSum).coerceAtLeast(0)
                    if (percent > maxAllowed) percent = maxAllowed

                    val newAmount = (percent / 100f) * total

                    isEditing = true
                    val formatted = "$percent%"
                    if (field.text.toString() != formatted) {
                        field.setText(formatted)
                        field.setSelection(field.text.length - 1)
                    }

                    // Aquí pasamos también los valores de alarmThreshold y alarmEnabled
                    val alarmThreshold = viewModel.alarmThresholdMap[category] ?: 0f
                    val alarmEnabled = viewModel.alarmEnabledMap[category] ?: false
                    viewModel.setCategoryBudget(category, newAmount, alarmThreshold.toFloat(), alarmEnabled)

                    isEditing = false
                }
            })
        }
    }

}
