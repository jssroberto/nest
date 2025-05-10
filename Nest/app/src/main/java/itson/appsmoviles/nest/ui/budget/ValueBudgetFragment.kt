package itson.appsmoviles.nest.ui.budget

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.checkbox.MaterialCheckBox
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.common.UiState
import itson.appsmoviles.nest.ui.home.HomeViewModel
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.showToast
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ValueBudgetFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(sharedMovementsViewModel) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()
    private lateinit var budgetViewModel: BudgetViewModel

    private val categoryBudgetLocalMap = mutableMapOf<CategoryType, Float>()

    private lateinit var textViewNetBalance: TextView
    private lateinit var textViewIncome: TextView
    private lateinit var textViewExpense: TextView
    private lateinit var editTextBudget: EditText

    private val currencyFormatter =
        DecimalFormat("$#,##0", DecimalFormatSymbols.getInstance(Locale.getDefault())).apply {
            roundingMode = RoundingMode.DOWN
            isGroupingUsed = true
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            negativePrefix = "-$"
            negativeSuffix = ""
        }
    private var isCurrencyFormatting = false
    private var isDataLoading = true
    private var hasLoadedInitialData = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        budgetViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {

                    return BudgetViewModel(
                        requireActivity().application,
                        sharedMovementsViewModel
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[BudgetViewModel::class.java]
        initializeViews(view)
        setupTotalBudgetInput()
        observeTotalBudget()
        observeAlarmData(view)
        budgetViewModel.loadCategoryAlarms()
        observeCategoryBudgets(view)
        setupAllCategoryInputs(view)
        observeOverviewState()
    }

    private fun initializeViews(view: View) {
        textViewNetBalance = view.findViewById(R.id.txt_budget_balance)
        textViewIncome = view.findViewById(R.id.txt_budget_income)
        textViewExpense = view.findViewById(R.id.txt_budget_expense)
        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("")
    }

    private fun setupTotalBudgetInput() {
        val totalBudgetWatcher = object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == currentText || isCurrencyFormatting || !hasLoadedInitialData) {
                    return
                }

                isCurrencyFormatting = true
                editTextBudget.removeTextChangedListener(this)

                val userInput = s.toString()
                val fallbackValue =
                    budgetViewModel.totalBudget.value?.let { BigDecimal(it.toString()) }
                        ?: BigDecimal.ZERO
                val processed =
                    processAndFormatCurrencyInput(userInput, currencyFormatter, fallbackValue)

                budgetViewModel.setTotalBudget(processed.parsedValue.toFloat())

                currentText = processed.displayString
                editTextBudget.setText(processed.displayString)
                editTextBudget.setSelection(editTextBudget.text.length.coerceAtMost(processed.displayString.length))

                editTextBudget.addTextChangedListener(this)
                isCurrencyFormatting = false
            }
        }
        editTextBudget.addTextChangedListener(totalBudgetWatcher)
        editTextBudget.setTag(R.id.monthly_budget_watcher_tag, totalBudgetWatcher)
    }

    private fun observeTotalBudget() {
        budgetViewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            if (budgetViewModel.totalBudget.value != null) {
                hasLoadedInitialData = true
            }


            if (isCurrencyFormatting || !hasLoadedInitialData) {
                return@observe
            }

            val currentEditTextValue = parseCurrency(editTextBudget.text.toString()).toFloat()
            if (currentEditTextValue != total || !editTextBudget.text.toString()
                    .startsWith("$") || (total == 0f && editTextBudget.text.toString()
                    .isNotBlank() && editTextBudget.text.toString() != "$0")
            ) {
                val watcher = editTextBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher
                formatAndSetEditText(editTextBudget, total, watcher)
            }
            adjustCategoryBudgetsIfExceedTotal(total)
        }
    }

    private fun adjustCategoryBudgetsIfExceedTotal(totalBudget: Float) {
        val sumOfCategories = categoryBudgetLocalMap.values.sum()
        if (sumOfCategories <= totalBudget || totalBudget < 0) return

        val scale = if (sumOfCategories > 0) totalBudget / sumOfCategories else 0f
        categoryBudgetLocalMap.forEach { (category, oldValue) ->
            val newValue = (oldValue * scale).coerceAtLeast(0f)

            budgetViewModel.setCategoryBudget(
                category,
                newValue,
                budgetViewModel.alarmThresholdMap[category] ?: 0f,
                budgetViewModel.alarmEnabledMap[category] ?: false
            )
        }
    }

    private fun observeAlarmData(view: View) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var firstAlarmDataEmission = true
                launch {
                    budgetViewModel.alarmThresholds.collect { thresholds ->
                        isDataLoading = true
                        thresholds.forEach { (category, value) ->
                            val etThreshold = view.findViewById<EditText>(
                                getEditTextIdForCategoryThreshold(category)
                            )
                            val watcher =
                                etThreshold.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher

                            val currentEditTextVal =
                                parseCurrency(etThreshold.text.toString()).toFloat()

                            if (currentEditTextVal != value.toFloat() ||
                                !etThreshold.text.toString().startsWith("$") ||
                                (value.toFloat() == 0f && etThreshold.text.toString() != formatCurrency(
                                    0f
                                )) ||
                                (firstAlarmDataEmission && !hasLoadedInitialData)
                            ) {
                                formatAndSetEditText(etThreshold, value.toFloat(), watcher)
                            }
                        }
                        if (firstAlarmDataEmission) firstAlarmDataEmission = false
                        isDataLoading = false
                    }
                }
                launch {
                    budgetViewModel.alarmEnabled.collect { enabledMap ->
                        enabledMap.forEach { (category, isChecked) ->
                            view.findViewById<MaterialCheckBox>(getSwitchIdForCategory(category)).isChecked =
                                isChecked
                        }
                    }
                }
            }
        }
    }

    private fun observeCategoryBudgets(view: View) {
        budgetViewModel.categoryBudgets.observe(viewLifecycleOwner) { categoryMap ->
            if (!hasLoadedInitialData && categoryMap.values.all { it == 0f } && categoryMap.isNotEmpty()) {

                if (budgetViewModel.totalBudget.value == null) return@observe
            }
            if (categoryMap.isNotEmpty()) hasLoadedInitialData = true


            categoryMap.forEach { (category, amount) ->
                categoryBudgetLocalMap[category] = amount
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val watcher = editText.getTag(R.id.et_food + category.ordinal) as? TextWatcher

                val currentEditTextVal = parseCurrency(editText.text.toString()).toFloat()
                if (currentEditTextVal != amount || !editText.text.toString()
                        .startsWith("$") || (amount == 0f && editText.text.toString() != "$0")
                ) {
                    formatAndSetEditText(editText, amount, watcher)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeOverviewState() {
        homeViewModel.overviewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {}
                is UiState.Success -> {
                    val overview = state.data
                    textViewIncome.text = formatCurrency(overview.totalIncome.toFloat())
                    textViewExpense.text = formatCurrency(overview.totalExpenses.toFloat())
                    textViewNetBalance.text = formatCurrency(overview.netBalance.toFloat())
                }

                is UiState.Error -> {
                    Log.e("ValueBudgetFragment", "Error loading budget: ${state.message}")
                    showToast(requireContext(), "Error loading budget: ${state.message}")
                }
            }
        }
    }

    private fun setupAllCategoryInputs(view: View) {
        val categoryFields = getCategoryEditTextMap(view)

        categoryFields.forEach { (category, editText) ->
            setupCategoryEditTextListener(category, editText, categoryFields, view)
            setupCategoryAlarmThresholdInput(category, view)
            setupCategoryAlarmSwitch(category, view)
        }
    }

    private fun getCategoryEditTextMap(view: View): Map<CategoryType, EditText> =
        CategoryType.values().associateWith { category ->
            view.findViewById<EditText>(getEditTextIdForCategory(category))
        }

    private fun setupCategoryEditTextListener(
        category: CategoryType,
        editText: EditText,
        allCategoryFields: Map<CategoryType, EditText>,
        view: View
    ) {
        val watcher = object : TextWatcher {
            private var currentText = ""

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == currentText || isCurrencyFormatting || !hasLoadedInitialData /* Allow changes if data is loaded */) {
                    return
                }
                isCurrencyFormatting = true
                editText.removeTextChangedListener(this)

                val userInput = s.toString()
                val currentCategoryValueInVm = budgetViewModel.categoryBudgets.value?.get(category)
                    ?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
                var processed = processAndFormatCurrencyInput(
                    userInput,
                    currencyFormatter,
                    currentCategoryValueInVm
                )
                var valueFromInput = processed.parsedValue

                val totalBudgetNum = budgetViewModel.totalBudget.value ?: 0f
                val totalBudgetValue = BigDecimal(totalBudgetNum.toString())

                val sumOfOthers = allCategoryFields
                    .filterKeys { it != category }
                    .values
                    .sumOf { otherEditText ->
                        parseCurrency(otherEditText.text.toString()).toDouble()
                    }
                val sumOfOthersBigDecimal = BigDecimal(sumOfOthers.toString())

                val maxAllowedForThisCategory =
                    (totalBudgetValue - sumOfOthersBigDecimal).coerceAtLeast(BigDecimal.ZERO)
                val finalClampedBigDecimal =
                    valueFromInput.min(maxAllowedForThisCategory).coerceAtLeast(BigDecimal.ZERO)

                budgetViewModel.setCategoryBudget(
                    category,
                    finalClampedBigDecimal.toFloat(),
                    budgetViewModel.alarmThresholdMap[category] ?: 0f,
                    budgetViewModel.alarmEnabledMap[category] ?: false
                )

                val displayStringToSet: String
                if (finalClampedBigDecimal.compareTo(valueFromInput) != 0) {
                    displayStringToSet = currencyFormatter.format(finalClampedBigDecimal)
                    if (valueFromInput > maxAllowedForThisCategory && totalBudgetNum > 0) {
                        showToast(
                            requireContext(),
                            "Valor ajustado para no exceder el presupuesto total o suma de otras categorías."
                        )
                    }
                } else {
                    displayStringToSet = processed.displayString
                }

                currentText = displayStringToSet
                editText.setText(displayStringToSet)
                editText.setSelection(editText.text.length.coerceAtMost(displayStringToSet.length))
                updateAlarmSwitchState(category, finalClampedBigDecimal.toFloat(), view)

                editText.addTextChangedListener(this)
                isCurrencyFormatting = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        editText.addTextChangedListener(watcher)
        editText.setTag(R.id.et_food + category.ordinal, watcher)
    }

    private fun updateAlarmSwitchState(category: CategoryType, budgetValue: Float, view: View) {
        val switchCategoryAlarm: MaterialCheckBox =
            view.findViewById(getSwitchIdForCategory(category))
        switchCategoryAlarm.isEnabled = budgetValue > 0f
        if (budgetValue <= 0f) {
            switchCategoryAlarm.isChecked = false

            budgetViewModel.setAlarmEnabled(category, false)
            persistAlarmChanges(
                category,
                budgetViewModel.alarmThresholdMap[category] ?: 0f,
                view,
                false
            )
        }
    }


    private fun setupCategoryAlarmThresholdInput(category: CategoryType, view: View) {
        val etCategoryAlarmThreshold: EditText =
            view.findViewById(getEditTextIdForCategoryThreshold(category))

        val alarmThresholdWatcher = object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == currentText || isCurrencyFormatting || isDataLoading || !hasLoadedInitialData) {
                    return
                }

                isCurrencyFormatting = true
                etCategoryAlarmThreshold.removeTextChangedListener(this)

                val userInput = s.toString()
                val existingAlarmValue =
                    budgetViewModel.alarmThresholds.value[category]?.let { BigDecimal(it.toString()) }
                        ?: BigDecimal.ZERO
                val processedInput =
                    processAndFormatCurrencyInput(userInput, currencyFormatter, existingAlarmValue)
                val parsedValueFromInput = processedInput.parsedValue

                val categoryBudget =
                    categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(
                        category
                    ) ?: 0f
                val categoryBudgetDecimal = BigDecimal(categoryBudget.toString())
                val clampedFinalValue =
                    parsedValueFromInput.min(categoryBudgetDecimal).coerceAtLeast(BigDecimal.ZERO)

                val originalInputExceededBudget =
                    parsedValueFromInput > categoryBudgetDecimal && categoryBudgetDecimal.compareTo(
                        BigDecimal.ZERO
                    ) >= 0

                budgetViewModel.setAlarmThreshold(category, clampedFinalValue.toFloat())

                val displayStringToSet: String
                if (clampedFinalValue.compareTo(parsedValueFromInput) != 0) {
                    displayStringToSet =   currencyFormatter.format(clampedFinalValue)
                    if (originalInputExceededBudget) {
                        showToast(
                            requireContext(),
                            "El umbral no puede superar el presupuesto de la categoría. Ajustado al máximo."
                        )
                    }
                } else {
                    displayStringToSet = processedInput.displayString
                }

                currentText = displayStringToSet
                etCategoryAlarmThreshold.setText(displayStringToSet)
                etCategoryAlarmThreshold.setSelection(
                    etCategoryAlarmThreshold.text.length.coerceAtMost(
                        displayStringToSet.length
                    )
                )

                etCategoryAlarmThreshold.addTextChangedListener(this)
                isCurrencyFormatting = false
            }
        }
        etCategoryAlarmThreshold.addTextChangedListener(alarmThresholdWatcher)

        etCategoryAlarmThreshold.setTag(
            R.id.editTextAlarmFood + category.ordinal,
            alarmThresholdWatcher
        )


        etCategoryAlarmThreshold.setOnFocusChangeListener { v, hasFocus ->

            if (isDataLoading || !hasLoadedInitialData) return@setOnFocusChangeListener

            if (!hasFocus) {
                val editText = v as EditText
                val currentTextInEditText = editText.text.toString()
                var valueFromEditText = parseCurrency(currentTextInEditText).toFloat()

                val categoryBudget =
                    categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(
                        category
                    ) ?: 0f
                var finalClampedValue =
                    valueFromEditText.coerceAtMost(categoryBudget).coerceAtLeast(0f)


                val watcher =
                    editText.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher


                if (currentTextInEditText != formatCurrency(finalClampedValue) || valueFromEditText != finalClampedValue) {
                    formatAndSetEditText(editText, finalClampedValue, watcher)
                    budgetViewModel.setAlarmThreshold(category, finalClampedValue)
                }


                val valueToPersist =
                    budgetViewModel.alarmThresholdMap[category] ?: finalClampedValue
                persistAlarmChanges(category, valueToPersist, view)
            }

        }
    }

    private fun setupCategoryAlarmSwitch(category: CategoryType, view: View) {
        val switchCategoryAlarm: MaterialCheckBox =
            view.findViewById(getSwitchIdForCategory(category))
        val etCategoryAlarmThreshold: EditText =
            view.findViewById(getEditTextIdForCategoryThreshold(category))

        switchCategoryAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isDataLoading || !hasLoadedInitialData) return@setOnCheckedChangeListener

            val thresholdValueString = etCategoryAlarmThreshold.text.toString()
            var thresholdValue = parseCurrency(thresholdValueString).toFloat()
            val categoryBudget =
                categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(
                    category
                ) ?: 0f

            if (thresholdValue <= 0f && isChecked) {
                switchCategoryAlarm.isChecked = false
                showToast(requireContext(), "No puedes activar una alarma con umbral 0 o inválido.")
                return@setOnCheckedChangeListener
            }


            if (thresholdValue > categoryBudget || thresholdValue < 0f || thresholdValueString != formatCurrency(
                    thresholdValue
                )
            ) {
                thresholdValue = thresholdValue.coerceAtMost(categoryBudget).coerceAtLeast(0f)
                val watcher =
                    etCategoryAlarmThreshold.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher
                formatAndSetEditText(etCategoryAlarmThreshold, thresholdValue, watcher)
                budgetViewModel.setAlarmThreshold(category, thresholdValue)
            }


            budgetViewModel.setAlarmEnabled(category, isChecked)
            persistAlarmChanges(category, thresholdValue, view, isChecked)
        }
    }

    private fun persistAlarmChanges(
        category: CategoryType,
        threshold: Float,
        view: View,
        isEnabledOverride: Boolean? = null
    ) {
        val isEnabled = isEnabledOverride ?: budgetViewModel.alarmEnabledMap[category]
        ?: view.findViewById<MaterialCheckBox>(
            getSwitchIdForCategory(category)
        ).isChecked
        budgetViewModel.persistAlarmThreshold(category, threshold.toDouble(), isEnabled)
    }

    private fun parseCurrency(input: String): BigDecimal {
        if (input.isBlank()) return BigDecimal.ZERO
        return try {

            val cleanString = input.replace("$", "")
                .replace(currencyFormatter.decimalFormatSymbols.groupingSeparator.toString(), "")

            val parsableString = cleanString.replace(
                currencyFormatter.decimalFormatSymbols.decimalSeparator.toString(),
                "."
            )
            BigDecimal(parsableString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }


    private fun formatCurrency(value: Float): String {
        val valueToFormat = if (value.isNaN()) 0f else value
        return currencyFormatter.format(BigDecimal(valueToFormat.toString()))
    }


    private fun formatAndSetEditText(
        editText: EditText,
        value: Float,
        watcherToRemoveTemporarily: TextWatcher? = null
    ) {
        val formatted = formatCurrency(value)
        val currentText = editText.text.toString()

        if (currentText != formatted) {
            watcherToRemoveTemporarily?.let { editText.removeTextChangedListener(it) }
            editText.setText(formatted)
            editText.setSelection(formatted.length.coerceAtMost(editText.text.length))
            watcherToRemoveTemporarily?.let { editText.addTextChangedListener(it) }
        } else if (editText.selectionStart != formatted.length && editText.isFocused) {
            editText.setSelection(formatted.length.coerceAtMost(editText.text.length))
        }
    }
}