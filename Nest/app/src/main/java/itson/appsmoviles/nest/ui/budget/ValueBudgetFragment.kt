package itson.appsmoviles.nest.ui.budget

import android.annotation.SuppressLint
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.checkbox.MaterialCheckBox
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.budget.CurrencyInputHelper.parseCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class ValueBudgetFragment : Fragment() {

    private val categoryBudgetLocalMap = mutableMapOf<CategoryType, Float>()
    private lateinit var editTextBudget: EditText
    private val currencyFormatter = DecimalFormat("#,##0.##").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumIntegerDigits = 7
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    private var isCurrencyFormatting = false
    private var isDataLoading = true
    private var hasLoadedData = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("")

        val totalBudgetWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isCurrencyFormatting || !hasLoadedData) return
                isCurrencyFormatting = true

                val parsedValue = parseCurrency(s.toString()).setScale(2, RoundingMode.DOWN).toFloat()
                viewModel.setTotalBudget(parsedValue)

                val formatted = "$" + currencyFormatter.format(BigDecimal(parsedValue.toString()))
                if (editTextBudget.text.toString() != formatted) {
                    editTextBudget.safeSetText(formatted, this)
                }

                isCurrencyFormatting = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        editTextBudget.addTextChangedListener(totalBudgetWatcher)

        viewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            if (!hasLoadedData && total == 0f) return@observe
            hasLoadedData = true

            val formatted = "$" + currencyFormatter.format(BigDecimal(total.toString()))
            if (editTextBudget.text.toString() != formatted) {
                editTextBudget.safeSetText(formatted, totalBudgetWatcher)
            }

            val sumOfCategories = categoryBudgetLocalMap.values.sum()
            if (sumOfCategories > total) {
                val scale = if (sumOfCategories > 0) total / sumOfCategories else 0f
                categoryBudgetLocalMap.forEach { (category, oldValue) ->
                    val newValue = oldValue * scale
                    viewModel.setCategoryBudget(
                        category,
                        newValue,
                        viewModel.alarmThresholdMap[category] ?: 0f,
                        viewModel.alarmEnabledMap[category] ?: false
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.alarmThresholds.collect { thresholds ->
                        isDataLoading = true
                        thresholds.forEach { (category, value) ->
                            val etThreshold = view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category))
                            val formatted = "$" + currencyFormatter.format(value)
                            if (etThreshold.text.toString() != formatted) {
                                etThreshold.setText(formatted)
                            }
                        }
                        isDataLoading = false // Fin de la carga de los datos
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

        viewModel.categoryBudgets.observe(viewLifecycleOwner) { categoryMap ->
            if (!hasLoadedData && categoryMap.values.all { it == 0f }) return@observe
            hasLoadedData = true

            categoryMap.forEach { (category, amount) ->
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val formatted = "$" + currencyFormatter.format(BigDecimal(amount.toString()))
                if (editText.text.toString() != formatted) {
                    editText.setText(formatted)
                    editText.setSelection(formatted.length.coerceAtMost(formatted.length))
                }
                categoryBudgetLocalMap[category] = amount
            }
        }

        setupCategoryInputs(view, viewModel)
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

        var isAlarmLoading = true

        // ⚠️ Esperar a que los umbrales se carguen antes de permitir edición
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.alarmThresholds.collect { thresholds ->
                    isAlarmLoading = true
                    thresholds.forEach { (category, value) ->
                        val etThreshold =
                            view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category))
                        val formatted = "$" + currencyFormatter.format(value)
                        if (etThreshold.text.toString() != formatted) {
                            etThreshold.setText(formatted)
                        }
                    }
                    isAlarmLoading = false
                }
            }
        }

        categoryFields.forEach { (category, editText) ->
            editText.setText("0")
            editText.addTextChangedListener(object : TextWatcher {
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

                    val alarmThreshold = viewModel.alarmThresholdMap[category] ?: 0f
                    val alarmEnabled = viewModel.alarmEnabledMap[category] ?: false

                    viewModel.setCategoryBudget(category, finalValue, alarmThreshold, alarmEnabled)

                    val formatted =
                        "$" + currencyFormatter.format(BigDecimal(finalValue.toString()))
                    if (editText.text.toString() != formatted) {
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    }

                    val switchCategoryAlarm: MaterialCheckBox =
                        view.findViewById(getSwitchIdForCategory(category))
                    val currentThreshold = parseCurrencyRaw(
                        view.findViewById<EditText>(getEditTextIdForCategoryThreshold(category)).text.toString()
                    )

                    if (finalValue == 0f) {
                        switchCategoryAlarm.isEnabled = false
                        switchCategoryAlarm.isChecked = false
                    } else {
                        switchCategoryAlarm.isEnabled = true
                    }

                    isCurrencyFormatting = false
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            val etCategoryAlarmThreshold: EditText =
                view.findViewById(getEditTextIdForCategoryThreshold(category))
            val switchCategoryAlarm: MaterialCheckBox =
                view.findViewById(getSwitchIdForCategory(category))

            etCategoryAlarmThreshold.setOnFocusChangeListener { _, hasFocus ->
                if (isAlarmLoading) return@setOnFocusChangeListener

                val raw = parseCurrency(etCategoryAlarmThreshold.text.toString())
                val categoryBudget = categoryBudgetLocalMap[category] ?: 0f
                val clampedRaw = raw.toFloat().coerceAtMost(categoryBudget)

                if (!hasFocus) {
                    val formattedWithSymbol = "$" + currencyFormatter.format(clampedRaw)
                    etCategoryAlarmThreshold.setText(formattedWithSymbol)
                    if (raw.toFloat() > categoryBudget) {
                        Toast.makeText(
                            requireContext(),
                            "El umbral no puede superar el presupuesto. Ajustado al máximo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.setAlarmThreshold(category, clampedRaw)
                    viewModel.persistAlarmThreshold(
                        category,
                        clampedRaw.toDouble(),
                        switchCategoryAlarm.isChecked
                    )
                } else {
                    etCategoryAlarmThreshold.setText(raw.toPlainString())
                    etCategoryAlarmThreshold.setSelection(etCategoryAlarmThreshold.text.length)
                }
            }

            etCategoryAlarmThreshold.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (isCurrencyFormatting || isAlarmLoading) return
                    isCurrencyFormatting = true

                    val raw = parseCurrency(s.toString())
                    val categoryBudget = categoryBudgetLocalMap[category] ?: 0f
                    val clamped = raw.toFloat().coerceAtMost(categoryBudget)

                    val formatted = "$" + currencyFormatter.format(BigDecimal(clamped.toString()))
                    if (etCategoryAlarmThreshold.text.toString() != formatted) {
                        etCategoryAlarmThreshold.setText(formatted)
                        etCategoryAlarmThreshold.setSelection(formatted.length)
                    }

                    viewModel.setAlarmThreshold(category, clamped)
                    isCurrencyFormatting = false
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            switchCategoryAlarm.setOnCheckedChangeListener { _, isChecked ->
                // Asegúrate de que los datos ya se hayan cargado antes de permitir cambios
                if (isDataLoading) return@setOnCheckedChangeListener

                // Obtén el valor actualizado del umbral de alarma directamente del EditText
                val raw = parseCurrency(etCategoryAlarmThreshold.text.toString())
                val thresholdValue = raw.toFloat()
                val categoryBudget = categoryBudgetLocalMap[category] ?: 0f


                // Comprobamos que el umbral no sea igual a 0 para poder activar la alarma
                if (thresholdValue <= 0f && isChecked) {
                    switchCategoryAlarm.isChecked = false
                    Toast.makeText(
                        requireContext(),
                        "No puedes activar una alarma con umbral 0.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnCheckedChangeListener
                }

                // Aseguramos que el valor del umbral no exceda el presupuesto de la categoría
                val clampedRaw = thresholdValue.coerceAtMost(categoryBudget)
                val formatted = "$" + currencyFormatter.format(BigDecimal(clampedRaw.toString()))

                // Actualizamos el texto del EditText con el valor clamped
                etCategoryAlarmThreshold.setText(formatted)

                // Guardamos el valor del umbral en el ViewModel
                viewModel.setAlarmThreshold(category, clampedRaw)

                // Persistimos los cambios (guardar en Firebase o almacenamiento local)
                viewModel.persistAlarmThreshold(category, clampedRaw.toDouble(), isChecked)

                // Actualizamos el estado de la alarma
                viewModel.setAlarmEnabled(category, isChecked)
            }


        }
    }


    private fun parseCurrency(input: String): BigDecimal =
        try { BigDecimal(input.replace(",", "").replace("$", "")) } catch (e: Exception) { BigDecimal.ZERO }

    private fun parseCurrencyRaw(input: String): BigDecimal =
        try { BigDecimal(input.replace(",", "")) } catch (e: Exception) { BigDecimal.ZERO }

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

    private fun EditText.safeSetText(formatted: String, watcher: TextWatcher) {
        removeTextChangedListener(watcher)
        setText(formatted)
        setSelection(formatted.length)
        addTextChangedListener(watcher)
    }
}
