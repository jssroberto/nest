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

class ValueBudgetFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    // Ensure sharedMovementsViewModel is initialized before HomeViewModel needs it
                    // For this example, assuming it's correctly handled by activityViewModels()
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

    private val currencyFormatter = DecimalFormat("#,##0.##").apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = true
        maximumIntegerDigits = 7
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    private var isCurrencyFormatting = false
    private var isDataLoading = true // Initialize as true, indicates initial data population
    private var hasLoadedInitialData = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        budgetViewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        initializeViews(view)
        setupTotalBudgetInput()
        observeTotalBudget()
        observeAlarmData(view) // Ensure this is called before setupAllCategoryInputs if it provides initial values
        budgetViewModel.loadCategoryAlarms()
        observeCategoryBudgets(view)
        setupAllCategoryInputs(view) // This will call the new setupCategoryAlarmThresholdInput
        observeOverviewState()
    }

    private fun initializeViews(view: View) {
        textViewNetBalance = view.findViewById(R.id.txt_budget_balance)
        textViewIncome = view.findViewById(R.id.txt_budget_income)
        textViewExpense = view.findViewById(R.id.txt_budget_expense)
        editTextBudget = view.findViewById(R.id.monthly_budget)
        editTextBudget.setText("") // Initial state, will be overwritten by observer if budget exists
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
                val fallbackValue = budgetViewModel.totalBudget.value?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
                val processed = processAndFormatCurrencyInput(userInput, currencyFormatter, fallbackValue)

                budgetViewModel.setTotalBudget(processed.parsedValue.toFloat())

                currentText = processed.displayString
                editTextBudget.setText(processed.displayString)
                editTextBudget.setSelection(editTextBudget.text.length.coerceAtMost(processed.displayString.length))

                editTextBudget.addTextChangedListener(this)
                isCurrencyFormatting = false
            }
        }
        editTextBudget.addTextChangedListener(totalBudgetWatcher)
        editTextBudget.setTag(R.id.monthly_budget_watcher_tag, totalBudgetWatcher) // Ensure this ID is unique and defined in ids.xml if not a view ID
    }

    private fun observeTotalBudget() {
        budgetViewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            if (!hasLoadedInitialData && total == 0f && editTextBudget.text.toString().isBlank()) {
                // Avoid race condition on initial load if field is empty and no budget is set.
                // If a budget (even 0f) IS set, we proceed to set hasLoadedInitialData.
                if (budgetViewModel.totalBudget.value == null) return@observe // Stricter check if 0f is a valid initial "unset" state
            }
            hasLoadedInitialData = true

            val currentEditTextValue = parseCurrency(editTextBudget.text.toString()).toFloat()
            if (currentEditTextValue != total || !editTextBudget.text.toString().startsWith("$") || (total == 0f && editTextBudget.text.toString().isNotBlank() && editTextBudget.text.toString() != "$0")) {
                val watcher = editTextBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher
                formatAndSetEditText(editTextBudget, total, watcher)
            }
            adjustCategoryBudgetsIfExceedTotal(total)
        }
    }

    private fun adjustCategoryBudgetsIfExceedTotal(totalBudget: Float) {
        val sumOfCategories = categoryBudgetLocalMap.values.sum()
        if (sumOfCategories <= totalBudget || totalBudget < 0) return // also guard against negative total budget

        val scale = if (sumOfCategories > 0) totalBudget / sumOfCategories else 0f
        categoryBudgetLocalMap.forEach { (category, oldValue) ->
            val newValue = (oldValue * scale).coerceAtLeast(0f) // Ensure non-negative
            // Update ViewModel, which should trigger observer for category EditText
            budgetViewModel.setCategoryBudget(
                category,
                newValue,
                budgetViewModel.alarmThresholdMap[category] ?: 0f, // Keep existing alarm values
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
                        isDataLoading = true // Indicate that we are programmatically setting data
                        thresholds.forEach { (category, value) ->
                            val etThreshold = view.findViewById<EditText>(
                                getEditTextIdForCategoryThreshold(category)
                            )
                            val watcher = etThreshold.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher

                            val currentEditTextVal = parseCurrency(etThreshold.text.toString()).toFloat()
                            // Update if different, or if not formatted, or if it's the first load and needs formatting (e.g. to $0)
                            if (currentEditTextVal != value.toFloat() ||
                                !etThreshold.text.toString().startsWith("$") ||
                                (value.toFloat() == 0f && etThreshold.text.toString() != formatCurrency(0f)) ||
                                (firstAlarmDataEmission && !hasLoadedInitialData) // Ensure initial values are set
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
                            view.findViewById<MaterialCheckBox>(getSwitchIdForCategory(category)).isChecked = isChecked
                        }
                    }
                }
            }
        }
    }

    private fun observeCategoryBudgets(view: View) {
        budgetViewModel.categoryBudgets.observe(viewLifecycleOwner) { categoryMap ->
            if (!hasLoadedInitialData && categoryMap.values.all { it == 0f } && categoryMap.isNotEmpty()) {
                // If all are zero and it's not an empty map (meaning data has arrived)
                // and we haven't loaded initial data yet, this might be the initial state.
                // Proceed if totalBudget has been loaded.
                if (budgetViewModel.totalBudget.value == null) return@observe
            }
            if (categoryMap.isNotEmpty()) hasLoadedInitialData = true


            categoryMap.forEach { (category, amount) ->
                categoryBudgetLocalMap[category] = amount // Keep local map in sync
                val editText = view.findViewById<EditText>(getEditTextIdForCategory(category))
                val watcher = editText.getTag(R.id.et_food + category.ordinal) as? TextWatcher

                val currentEditTextVal = parseCurrency(editText.text.toString()).toFloat()
                if (currentEditTextVal != amount || !editText.text.toString().startsWith("$") || (amount == 0f && editText.text.toString() != "$0")) {
                    formatAndSetEditText(editText, amount, watcher)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeOverviewState() {
        homeViewModel.overviewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* Handle loading state if needed */ }
                is UiState.Success -> {
                    val overview = state.data
                    textViewIncome.text = "$${overview.totalIncome.toInt()}" // Consider formatting like other currency
                    textViewExpense.text = "$${overview.totalExpenses.toInt()}"
                    textViewNetBalance.text = "$${overview.netBalance.toInt()}"
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
        // isDataLoading is managed by observers primarily.
        // Initial text for category EditTexts will be set by observeCategoryBudgets

        categoryFields.forEach { (category, editText) ->
            // editText.setText("$0") // Initial text set by observer to avoid conflicts
            setupCategoryEditTextListener(category, editText, categoryFields, view)
            setupCategoryAlarmThresholdInput(category, view) // This will use the new implementation
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
                val currentCategoryValueInVm = budgetViewModel.categoryBudgets.value?.get(category)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
                var processed = processAndFormatCurrencyInput(userInput, currencyFormatter, currentCategoryValueInVm)
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

                val maxAllowedForThisCategory = (totalBudgetValue - sumOfOthersBigDecimal).coerceAtLeast(BigDecimal.ZERO)
                val finalClampedBigDecimal = valueFromInput.min(maxAllowedForThisCategory).coerceAtLeast(BigDecimal.ZERO)

                budgetViewModel.setCategoryBudget(
                    category,
                    finalClampedBigDecimal.toFloat(),
                    budgetViewModel.alarmThresholdMap[category] ?: 0f,
                    budgetViewModel.alarmEnabledMap[category] ?: false
                )

                val displayStringToSet: String
                if (finalClampedBigDecimal.compareTo(valueFromInput) != 0) { // Clamped
                    displayStringToSet = "$" + currencyFormatter.format(finalClampedBigDecimal)
                    if (valueFromInput > maxAllowedForThisCategory && totalBudgetNum > 0) { // only show toast if clamping was due to total budget limit
                        showToast(requireContext(), "Valor ajustado para no exceder el presupuesto total o suma de otras categorías.")
                    }
                } else { // Not clamped or clamped to itself (e.g. negative to zero)
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
        editText.setTag(R.id.et_food + category.ordinal, watcher) // Make sure these R.id tags are correct and unique per category type
    }

    private fun updateAlarmSwitchState(category: CategoryType, budgetValue: Float, view: View) {
        val switchCategoryAlarm: MaterialCheckBox = view.findViewById(getSwitchIdForCategory(category))
        switchCategoryAlarm.isEnabled = budgetValue > 0f
        if (budgetValue <= 0f) { // Also ensure it's disabled if budget becomes 0 or less
            switchCategoryAlarm.isChecked = false
            // Persist this change if needed, or let setAlarmEnabled handle it
            budgetViewModel.setAlarmEnabled(category, false)
            persistAlarmChanges(category, budgetViewModel.alarmThresholdMap[category] ?: 0f, view, false)
        }
    }

    // --- REFACTORED METHOD ---
    private fun setupCategoryAlarmThresholdInput(category: CategoryType, view: View) {
        val etCategoryAlarmThreshold: EditText = view.findViewById(getEditTextIdForCategoryThreshold(category))

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
                val existingAlarmValue = budgetViewModel.alarmThresholds.value[category]?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO
                val processedInput = processAndFormatCurrencyInput(userInput, currencyFormatter, existingAlarmValue)
                val parsedValueFromInput = processedInput.parsedValue

                val categoryBudget = categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(category) ?: 0f
                val categoryBudgetDecimal = BigDecimal(categoryBudget.toString())
                val clampedFinalValue = parsedValueFromInput.min(categoryBudgetDecimal).coerceAtLeast(BigDecimal.ZERO)

                val originalInputExceededBudget = parsedValueFromInput > categoryBudgetDecimal && categoryBudgetDecimal.compareTo(BigDecimal.ZERO) >= 0

                budgetViewModel.setAlarmThreshold(category, clampedFinalValue.toFloat())

                val displayStringToSet: String
                if (clampedFinalValue.compareTo(parsedValueFromInput) != 0) { // Value was clamped
                    displayStringToSet = "$" + currencyFormatter.format(clampedFinalValue)
                    if (originalInputExceededBudget) { // Show toast only if clamping was due to exceeding category budget
                        showToast(
                            requireContext(),
                            "El umbral no puede superar el presupuesto de la categoría. Ajustado al máximo."
                        )
                    }
                } else { // Value not meaningfully clamped from user's valid input perspective
                    displayStringToSet = processedInput.displayString
                }

                currentText = displayStringToSet
                etCategoryAlarmThreshold.setText(displayStringToSet)
                etCategoryAlarmThreshold.setSelection(etCategoryAlarmThreshold.text.length.coerceAtMost(displayStringToSet.length))

                etCategoryAlarmThreshold.addTextChangedListener(this)
                isCurrencyFormatting = false
            }
        }
        etCategoryAlarmThreshold.addTextChangedListener(alarmThresholdWatcher)
        // Ensure this ID is unique and defined, e.g. in an ids.xml if not a view ID
        etCategoryAlarmThreshold.setTag(R.id.editTextAlarmFood + category.ordinal, alarmThresholdWatcher)


        etCategoryAlarmThreshold.setOnFocusChangeListener { v, hasFocus ->
            // isDataLoading check might be redundant if hasLoadedInitialData is robustly handled by watchers
            if (isDataLoading || !hasLoadedInitialData) return@setOnFocusChangeListener

            if (!hasFocus) { // Focus Lost
                val editText = v as EditText
                val currentTextInEditText = editText.text.toString()
                var valueFromEditText = parseCurrency(currentTextInEditText).toFloat()

                val categoryBudget = categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(category) ?: 0f
                var finalClampedValue = valueFromEditText.coerceAtMost(categoryBudget).coerceAtLeast(0f)

                // Ensure EditText visually reflects the final clamped value, especially if it was blank or invalid
                // The watcher associated with the EditText
                val watcher = editText.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher

                // If the text needs reformatting (e.g. "" -> "$0") or value changed by clamp
                if (currentTextInEditText != formatCurrency(finalClampedValue) || valueFromEditText != finalClampedValue) {
                    formatAndSetEditText(editText, finalClampedValue, watcher) // This will format and set text
                    budgetViewModel.setAlarmThreshold(category, finalClampedValue) // Ensure VM is updated
                }

                // Persist the value that's confirmed in the VM (or finalClampedValue as best effort)
                val valueToPersist = budgetViewModel.alarmThresholdMap[category] ?: finalClampedValue
                persistAlarmChanges(category, valueToPersist, view)
            }
            // No special behavior for gaining focus, to match other EditTexts
        }
    }

    private fun setupCategoryAlarmSwitch(category: CategoryType, view: View) {
        val switchCategoryAlarm: MaterialCheckBox = view.findViewById(getSwitchIdForCategory(category))
        val etCategoryAlarmThreshold: EditText = view.findViewById(getEditTextIdForCategoryThreshold(category))

        switchCategoryAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isDataLoading || !hasLoadedInitialData) return@setOnCheckedChangeListener // Guard against programmatic changes during load

            val thresholdValueString = etCategoryAlarmThreshold.text.toString()
            var thresholdValue = parseCurrency(thresholdValueString).toFloat()
            val categoryBudget = categoryBudgetLocalMap[category] ?: budgetViewModel.categoryBudgets.value?.get(category) ?: 0f

            if (thresholdValue <= 0f && isChecked) {
                switchCategoryAlarm.isChecked = false // Revert check
                showToast(requireContext(), "No puedes activar una alarma con umbral 0 o inválido.")
                return@setOnCheckedChangeListener
            }

            // Ensure threshold is valid and clamped before enabling
            if (thresholdValue > categoryBudget || thresholdValue < 0f || thresholdValueString != formatCurrency(thresholdValue) ) {
                thresholdValue = thresholdValue.coerceAtMost(categoryBudget).coerceAtLeast(0f)
                val watcher = etCategoryAlarmThreshold.getTag(R.id.editTextAlarmFood + category.ordinal) as? TextWatcher
                formatAndSetEditText(etCategoryAlarmThreshold, thresholdValue, watcher) // Update EditText if clamped
                budgetViewModel.setAlarmThreshold(category, thresholdValue) // Update VM
            }


            budgetViewModel.setAlarmEnabled(category, isChecked)
            persistAlarmChanges(category, thresholdValue, view, isChecked)
        }
    }

    private fun persistAlarmChanges(
        category: CategoryType,
        threshold: Float,
        view: View, // View might not be needed if switch state comes from ViewModel
        isEnabledOverride: Boolean? = null
    ) {
        val isEnabled = isEnabledOverride ?: budgetViewModel.alarmEnabledMap[category] ?: view.findViewById<MaterialCheckBox>(
            getSwitchIdForCategory(category)
        ).isChecked
        budgetViewModel.persistAlarmThreshold(category, threshold.toDouble(), isEnabled)
    }

    private fun parseCurrency(input: String): BigDecimal {
        if (input.isBlank()) return BigDecimal.ZERO // Handle blank input explicitly as zero
        return try {
            // More robust parsing: remove currency symbol and grouping separators based on formatter's locale
            val cleanString = input.replace("$", "").replace(currencyFormatter.decimalFormatSymbols.groupingSeparator.toString(), "")
            // Replace decimal separator with dot for BigDecimal if it's different
            val parsableString = cleanString.replace(currencyFormatter.decimalFormatSymbols.decimalSeparator.toString(), ".")
            BigDecimal(parsableString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }


    private fun formatCurrency(value: Float): String {
        // Ensure we are formatting a non-negative value unless negatives are explicitly allowed
        val valueToFormat = if (value.isNaN()) 0f else value//value.coerceAtLeast(0f)
        return "$" + currencyFormatter.format(BigDecimal(valueToFormat.toString()))
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
            // Set selection after text is fully set
            editText.setSelection(formatted.length.coerceAtMost(editText.text.length))
            watcherToRemoveTemporarily?.let { editText.addTextChangedListener(it) }
        } else if (editText.selectionStart != formatted.length && editText.isFocused) {
            // Only adjust selection if text is same AND field is focused (to avoid issues during initial load)
            // Or, more simply, always try to set selection if text is already correct
            editText.setSelection(formatted.length.coerceAtMost(editText.text.length))
        }
    }
}