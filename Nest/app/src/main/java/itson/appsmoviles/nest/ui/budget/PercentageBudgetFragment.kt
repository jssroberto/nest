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
    ): View? = inflater.inflate(R.layout.fragment_budget, container, false)

    // ... mismo import y declaración de clase

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("$0.00")

        // Observador del presupuesto total
        viewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            val formatted = currencyFormatter.format(BigDecimal(total.toString()))
            if (editTextBudget.text.toString() != formatted) {
                editTextBudget.setText(formatted)
                editTextBudget.setSelection(formatted.length)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.alarmThresholds.collect { thresholds ->
                        thresholds.forEach { (category, value) ->
                            val editText = view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category))
                            val formatted = currencyFormatter.format(value)
                            editText.setText(formatted)
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

        // Llama al cargar datos
        viewModel.loadCategoryAlarms()

        // Observador de porcentajes por categoría (modificado)
        viewModel.categoryPercentages.observe(viewLifecycleOwner) { categoryMap ->
            val percentFormatter = DecimalFormat("##0.##'%'")
            categoryMap.forEach { (category, percent) ->
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val formatted = percentFormatter.format(percent)
                if (editText.text.toString() != formatted) {
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }
            }
        }

        setupCategoryInputs(view, viewModel)

        // TextWatcher del presupuesto total
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

        val percentFormatter = DecimalFormat("##0.##'%'")


        categoryFields.forEach { (category, editText) ->
            editText.setText("0%")

            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (isCurrencyFormatting) return
                    isCurrencyFormatting = true

                    val inputPercent = parsePercentage(s.toString())
                    val totalBudget = parseCurrency(editTextBudget.text.toString()).toFloat()

                    // Calcular suma de los demás porcentajes
                    val sumOfOthers = categoryFields
                        .filter { it.key != category }
                        .map { parsePercentage(it.value.text.toString()) }
                        .sum()

                    val maxAllowed = (100f - sumOfOthers).coerceAtLeast(0f)
                    val finalPercent = inputPercent.coerceAtMost(maxAllowed)
                    val amount = (totalBudget * finalPercent / 100f)

                    val alarmThreshold = viewModel.alarmThresholdMap[category] ?: 0f
                    val alarmEnabled = viewModel.alarmEnabledMap[category] ?: false

                    viewModel.setCategoryBudget(category, amount, alarmThreshold, alarmEnabled)

                    val formatted = percentFormatter.format(BigDecimal(finalPercent.toDouble()))
                    if (editText.text.toString() != formatted) {
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    }

                    isCurrencyFormatting = false
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Configuración del umbral de alarma (igual que antes)
            val etCategoryAlarmThreshold: EditText = view.findViewById(getEditTextIdForCategoryThreshold(category))
            val switchCategoryAlarm: MaterialCheckBox = view.findViewById(getSwitchIdForCategory(category))

            etCategoryAlarmThreshold.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val raw = parseCurrency(etCategoryAlarmThreshold.text.toString())
                    etCategoryAlarmThreshold.setText(raw.toPlainString())
                    etCategoryAlarmThreshold.setSelection(etCategoryAlarmThreshold.text.length)
                } else {
                    val raw = parseCurrency(etCategoryAlarmThreshold.text.toString())
                    val formatted = currencyFormatter.format(raw)
                    etCategoryAlarmThreshold.setText(formatted)
                    val isChecked = switchCategoryAlarm.isChecked
                    viewModel.setAlarmThreshold(category, raw.toFloat())
                    viewModel.persistAlarmThreshold(category, raw.toDouble(), isChecked)
                }
            }

            switchCategoryAlarm.setOnCheckedChangeListener { _, isChecked ->
                val raw = parseCurrencyRaw(etCategoryAlarmThreshold.text.toString())
                viewModel.setAlarmEnabled(category, isChecked)
                viewModel.persistAlarmThreshold(category, raw.toDouble(), isChecked)
            }
        }
    }

    private fun parsePercentage(input: String): Float {
        return try {
            input.replace("%", "")
                .replace(",", ".")
                .trim()
                .toFloat()
        } catch (e: Exception) {
            0f
        }
    }


    private fun parseCurrency(input: String): BigDecimal {
        return try {
            BigDecimal(input.replace("[^\\d]".toRegex(), "")).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseCurrencyRaw(input: String): BigDecimal {
        return try {
            val clean = input.replace("[^\\d]".toRegex(), "")
            if (clean.isEmpty()) BigDecimal.ZERO else BigDecimal(clean).movePointLeft(2)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

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
