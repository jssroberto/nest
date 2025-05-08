package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.checkbox.MaterialCheckBox
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale


class PercentageBudgetFragment : Fragment() {

    private lateinit var editTextBudget: EditText
    private var isFormatting = false
    private var isThresholdChanged = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("0")

        setupObservers(view, viewModel)
        setupCategoryInputs(view, viewModel)

        editTextBudget.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isThresholdChanged = true
                if (isFormatting) return
                isFormatting = true

                val parsedValue = parseCurrency(s.toString()).toFloat()
                viewModel.setTotalBudget(parsedValue)

                val formatted = formatCurrencyInput(parsedValue)
                if (editTextBudget.text.toString() != formatted) {
                    editTextBudget.setText(formatted)
                    editTextBudget.setSelection(formatted.length)
                }

                isFormatting = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupObservers(view: View, viewModel: BudgetViewModel) {
        viewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            val formatted = formatCurrencyInput(total)
            if (editTextBudget.text.toString() != formatted) {
                editTextBudget.setText(formatted)
                editTextBudget.setSelection(formatted.length)
            }
        }

        viewModel.categoryPercentages.observe(viewLifecycleOwner) { categoryMap ->
            categoryMap.forEach { (category, percent) ->
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val formatted = formatPercentage(percent)
                if (editText.text.toString() != formatted) {
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.alarmThresholds.collect { thresholds ->
                        thresholds.forEach { (category, value) ->
                            val editText = view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category))
                            val formatted = formatCurrencyInput(value)
                            if (editText.text.toString() != formatted) {
                                editText.setText(formatted)
                            }
                        }


                }
                }
                launch {
                    viewModel.alarmEnabled.collect { enabledMap ->
                        enabledMap.forEach { (category, isChecked) ->
                            val switch = view.findViewById<MaterialCheckBox>(getSwitchIdForCategory(category))
                            switch.isChecked = isChecked
                        }
                    }
                }
            }
        }

        viewModel.loadCategoryAlarms()
    }

    private fun setupCategoryInputs(view: View, viewModel: BudgetViewModel) {
        val categoryFields = CategoryType.values().associateWith { category ->
            view.findViewById<EditText>(getEditTextIdForCategory(category))
        }

        categoryFields.forEach { (category, editText) ->
            editText.setText("0")

            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (isFormatting) return
                    isFormatting = true

                    val inputPercent = parsePercentage(s.toString())
                    val totalBudget = parseCurrency(editTextBudget.text.toString()).toFloat()

                    val sumOthers = categoryFields.filterKeys { it != category }
                        .values.sumOf { parsePercentage(it.text.toString()).toDouble() }.toFloat()

                    val maxAllowed = (100f - sumOthers).coerceAtLeast(0f)
                    val finalPercent = inputPercent.coerceAtMost(maxAllowed)
                    val amount = totalBudget * finalPercent / 100f

                    val alarmThreshold = viewModel.alarmThresholdMap[category] ?: 0f
                    val alarmEnabled = viewModel.alarmEnabledMap[category] ?: false

                    viewModel.setCategoryBudget(category, amount, alarmThreshold, alarmEnabled)

                    val formatted = formatPercentage(finalPercent)
                    if (editText.text.toString() != formatted) {
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    }

                    isFormatting = false
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            val etAlarm = view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category))
            val switch = view.findViewById<MaterialCheckBox>(getSwitchIdForCategory(category))

            etAlarm.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val raw = parseCurrencyRaw(etAlarm.text.toString())
                    val floatValue = raw.toFloat()
                    val isChecked = switch.isChecked

                    etAlarm.setText(formatCurrencyInput(floatValue))
                    viewModel.setAlarmThreshold(category, floatValue)
                    viewModel.persistAlarmThreshold(category, raw.toDouble(), isChecked)
                }
            }



            switch.setOnCheckedChangeListener { _, isChecked ->
                val raw = parseCurrencyRaw(etAlarm.text.toString())
                viewModel.setAlarmEnabled(category, isChecked)
                viewModel.persistAlarmThreshold(category, raw.toDouble(), isChecked)
            }
        }
    }

    private fun parseCurrency(input: String): BigDecimal {
        return try {
            BigDecimal(input.replace("[^\\d]".toRegex(), ""))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseCurrencyRaw(input: String): BigDecimal {
        return try {
            val clean = input.replace("[^\\d]".toRegex(), "")
            if (clean.isEmpty()) BigDecimal.ZERO else BigDecimal(clean)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parsePercentage(input: String): Float {
        return try {
            input.replace("%", "").replace(",", ".").trim().toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    private fun formatCurrencyInput(value: Float): String = value.toString().removeSuffix(".0")

    private fun formatPercentage(value: Float): String = value.toString().removeSuffix(".0")

    private fun getEditTextIdForCategory(category: CategoryType) = when (category) {
        CategoryType.FOOD -> R.id.et_food
        CategoryType.LIVING -> R.id.et_home
        CategoryType.HEALTH -> R.id.et_health
        CategoryType.RECREATION -> R.id.et_recreation
        CategoryType.TRANSPORT -> R.id.et_transport
        CategoryType.OTHER -> R.id.et_others
    }

    private fun getEditTextIdForCategoryThreshold(category: CategoryType) = when (category) {
        CategoryType.FOOD -> R.id.editTextAlarmFood
        CategoryType.LIVING -> R.id.editTextAlarmHome
        CategoryType.HEALTH -> R.id.editTextAlarmHealth
        CategoryType.RECREATION -> R.id.editTextAlarmRecreation
        CategoryType.TRANSPORT -> R.id.editTextAlarmTransport
        CategoryType.OTHER -> R.id.editTextAlarmOthers
    }

    private fun getSwitchIdForCategory(category: CategoryType) = when (category) {
        CategoryType.FOOD -> R.id.checkBoxAlarmFood
        CategoryType.LIVING -> R.id.checkBoxAlarmHome
        CategoryType.HEALTH -> R.id.checkBoxAlarmHealth
        CategoryType.RECREATION -> R.id.checkBoxAlarmRecreation
        CategoryType.TRANSPORT -> R.id.checkBoxAlarmTransport
        CategoryType.OTHER -> R.id.checkBoxAlarmOthers
    }
}
