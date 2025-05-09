package itson.appsmoviles.nest.ui.budget

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
import java.text.NumberFormat
import java.util.Locale


class PercentageBudgetFragment : Fragment() {

    private val budgetViewModel: BudgetViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    val sharedVM =
                        ViewModelProvider(requireActivity())[SharedMovementsViewModel::class.java]
                    return HomeViewModel(sharedVM) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private lateinit var editTextTotalBudget: EditText
    private lateinit var textViewNetBalance: TextView
    private lateinit var textViewIncome: TextView
    private lateinit var textViewExpense: TextView

    private val categoryPercentageEditTexts = mutableMapOf<CategoryType, EditText>()
    private val categoryAlarmThresholdEditTexts = mutableMapOf<CategoryType, EditText>()
    private val categoryAlarmSwitches = mutableMapOf<CategoryType, MaterialCheckBox>()

    private val currencyFormatter =
        DecimalFormat("$#,##0.00", DecimalFormatSymbols.getInstance(Locale.getDefault())).apply {
            roundingMode = RoundingMode.DOWN
            isGroupingUsed = true // Use grouping for currency display
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    private val percentageFormatter = DecimalFormat(
        "#,##0.0'%'",
        DecimalFormatSymbols.getInstance(Locale.getDefault())
    ).apply { // Example: 25.5%
        roundingMode = RoundingMode.HALF_UP
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }

    private var isCurrencyFormatting = false // Used for Total Budget and Alarm Thresholds
    private var isPercentageFormatting = false // Used for Category Percentage EditTexts
    private var hasLoadedInitialData = false
    private var isObserverUpdating =
        false // Flag to prevent infinite loops from ViewModel observers

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupTotalBudgetInput()
        observeTotalBudget()

        // Order matters: Observe first to populate fields, then setup listeners
        observeCategoryPercentages()
        observeAlarmData() // Ensure alarm data is observed

        setupCategoryPercentageInputs() // Adds TextWatchers and FocusListeners
        setupCategoryAlarmInputsAndSwitches() // Adds TextWatchers, FocusListeners, and Check Listeners

        observeOverviewState()

        // Manually trigger initial load if needed, or rely on ViewModel init
        // budgetViewModel.loadBudgetAndAlarms() // Assuming a method like this exists
    }

    private fun initializeViews(view: View) {
        editTextTotalBudget = view.findViewById(R.id.monthly_budget)
        textViewNetBalance = view.findViewById(R.id.txt_budget_balance)
        textViewIncome = view.findViewById(R.id.txt_budget_income)
        textViewExpense = view.findViewById(R.id.txt_budget_expense)

        CategoryType.values().forEach { category ->
            categoryPercentageEditTexts[category] =
                view.findViewById(getEditTextIdForCategory(category))
            categoryAlarmThresholdEditTexts[category] =
                view.findViewById(getEditTextIdForCategoryThreshold(category))
            categoryAlarmSwitches[category] = view.findViewById(getSwitchIdForCategory(category))
        }
    }

    // --- Total Budget Handling ---
    private fun setupTotalBudgetInput() {
        val totalBudgetWatcher = object : TextWatcher {
            private var currentText = "" // Keep track to avoid unnecessary updates

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == currentText || isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) {
                    // Only process if text actually changed, not formatting internally, not observer update, and initial data loaded
                    return
                }

                isCurrencyFormatting = true
                editTextTotalBudget.removeTextChangedListener(this)

                val userInput = s.toString()
                // Fallback to current ViewModel value if parsing fails
                val fallbackValue =
                    budgetViewModel.totalBudget.value?.let { BigDecimal(it.toString()) }
                        ?: BigDecimal.ZERO
                val processedInput =
                    processAndFormatCurrencyInput(userInput, currencyFormatter, fallbackValue)

                // Update ViewModel with the parsed value
                budgetViewModel.setTotalBudget(processedInput.parsedValue.toFloat())

                // Update the EditText immediately with the formatted string from processing
                currentText = processedInput.displayString
                editTextTotalBudget.setText(currentText)
                // Set selection to the end
                editTextTotalBudget.setSelection(
                    editTextTotalBudget.text.length.coerceAtMost(
                        currentText.length
                    )
                )

                editTextTotalBudget.addTextChangedListener(this)
                isCurrencyFormatting = false
            }
        }
        editTextTotalBudget.addTextChangedListener(totalBudgetWatcher)
        // Ensure this ID is unique and defined
        editTextTotalBudget.setTag(R.id.monthly_budget_watcher_tag, totalBudgetWatcher)

        editTextTotalBudget.setOnFocusChangeListener { _, hasFocus ->
            if (isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) return@setOnFocusChangeListener
            val watcher =
                editTextTotalBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher

            if (!hasFocus) { // Lost focus
                // Format text based on the current ViewModel value
                val vmValue = budgetViewModel.totalBudget.value ?: 0f
                safeSetEditText(
                    editTextTotalBudget,
                    formatCurrency(vmValue),
                    watcher,
                    true,
                    isCurrencyFormatting
                ) { isCurrencyFormatting = it }
            } else { // Gained focus - potentially format to raw number for easier editing
                // Get current value from ViewModel or EditText
                val currentValue = budgetViewModel.totalBudget.value ?: parseCurrency(
                    editTextTotalBudget.text.toString()
                ).toFloat()
                // Format without currency symbol and grouping for editing, if value > 0
                val rawString = if (currentValue > 0) {
                    DecimalFormat(
                        "#0.##",
                        DecimalFormatSymbols.getInstance(Locale.getDefault())
                    ).format(currentValue)
                } else {
                    ""
                }
                safeSetEditText(
                    editTextTotalBudget,
                    rawString,
                    watcher,
                    false,
                    isCurrencyFormatting
                ) { isCurrencyFormatting = it }
            }
        }
    }

    private fun observeTotalBudget() {
        budgetViewModel.totalBudget.observe(viewLifecycleOwner) { total ->
            // This observer updates the UI when the ViewModel's total budget changes,
            // but we want to avoid interfering if the change came from the user typing
            if (isCurrencyFormatting || isObserverUpdating) return@observe

            isObserverUpdating = true // Indicate update is from observer
            val formattedTotal = formatCurrency(total)
            // Only update EditText if the text content doesn't match the formatted value
            if (editTextTotalBudget.text.toString() != formattedTotal) {
                val watcher =
                    editTextTotalBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher
                // Use safeSetEditText, setting cursor at end, managed by the formatting flag
                safeSetEditText(
                    editTextTotalBudget,
                    formattedTotal,
                    watcher,
                    true,
                    isCurrencyFormatting
                ) { isCurrencyFormatting = it }
            }

            // Mark data as loaded once we receive a total budget value (even 0 implies data loaded)
            if (budgetViewModel.totalBudget.value != null) hasLoadedInitialData = true

            isObserverUpdating = false
        }
    }


    // --- Category Percentages Display & Editing ---
    private fun observeCategoryPercentages() {
        budgetViewModel.categoryPercentages.observe(viewLifecycleOwner) { percentagesMap ->
            // Prevent observer from updating while user is actively typing a percentage
            if (isPercentageFormatting || isObserverUpdating) return@observe

            isObserverUpdating = true // Indicate update is from observer
            percentagesMap.forEach { (category, percent) -> // percent is 0-100 e.g. 25.0 for 25%
                categoryPercentageEditTexts[category]?.let { editText ->
                    val formattedPercentage = formatPercentage(percent)
                    // Only update EditText if the text content doesn't match the formatted value
                    if (editText.text.toString() != formattedPercentage) {
                        val watcher =
                            editText.getTag(R.id.percentage_watcher_tag_prefix + category.ordinal) as? TextWatcher
                        // Use safeSetEditText, setting cursor at end, managed by the formatting flag
                        safeSetEditText(
                            editText,
                            formattedPercentage,
                            watcher,
                            true,
                            isPercentageFormatting
                        ) { isPercentageFormatting = it }
                    }
                }
            }
            // Mark data as loaded if we receive percentage data
            if (percentagesMap.isNotEmpty()) hasLoadedInitialData = true

            isObserverUpdating = false
        }
    }

    private fun setupCategoryPercentageInputs() {
        categoryPercentageEditTexts.forEach { (category, editText) ->
            val watcherTag =
                R.id.percentage_watcher_tag_prefix + category.ordinal // Unique tag per category
            val watcher = object : TextWatcher {
                private var currentText = ""

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Guards against internal formatting/observer updates/initial load
                    if (s.toString() == currentText || isPercentageFormatting || isObserverUpdating || !hasLoadedInitialData) {
                        return
                    }

                    isPercentageFormatting = true // Lock for this EditText's formatting
                    editText.removeTextChangedListener(this)

                    val userInput = s.toString()
                    val enteredPercentage =
                        parsePercentageInput(userInput) // Parses "25.5%" to 25.5f

                    val totalBudget = budgetViewModel.totalBudget.value ?: 0f

                    // Clamp percentage based on 100% total
                    val otherPercentagesSum = budgetViewModel.categoryPercentages.value
                        ?.filterKeys { it != category }
                        ?.values?.sum() ?: 0f

                    val maxAllowedPercentage = (100f - otherPercentagesSum).coerceAtLeast(0f)
                    var finalValidPercentage = enteredPercentage.coerceAtMost(maxAllowedPercentage)
                    // Round the clamped percentage to match the formatter's precision (1 decimal)
                    finalValidPercentage = finalValidPercentage.toBigDecimal()
                        .setScale(percentageFormatter.maximumFractionDigits, RoundingMode.HALF_UP)
                        .toFloat()


                    // Calculate the actual amount based on the clamped percentage
                    val actualAmount = if (totalBudget > 0f) {
                        (totalBudget * finalValidPercentage / 100f)
                            .toBigDecimal()
                            .setScale(currencyFormatter.maximumFractionDigits, RoundingMode.HALF_UP)
                            .toFloat()
                    } else {
                        0f
                    }

                    // Format the clamped percentage for display *immediately*
                    val displayStringToSet = formatPercentage(finalValidPercentage)

                    // Update EditText visually
                    currentText = displayStringToSet
                    editText.setText(displayStringToSet)
                    // Set selection to the end after formatting
                    editText.setSelection(editText.text.length.coerceAtMost(displayStringToSet.length))

                    editText.addTextChangedListener(this)
                    isPercentageFormatting = false // Release the lock

                    // Update ViewModel with the calculated amount
                    // This will trigger the observer, but the observer is guarded by isPercentageFormatting
                    budgetViewModel.setCategoryBudget(
                        category, actualAmount,
                        budgetViewModel.alarmThresholdMap[category] ?: 0f,
                        budgetViewModel.alarmEnabledMap[category] ?: false
                    )

                    // Provide feedback if clamping occurred
                    if (enteredPercentage > finalValidPercentage && totalBudget > 0f && Math.abs(
                            enteredPercentage - finalValidPercentage
                        ) > 0.05f
                    ) {
                        showToast(
                            requireContext(),
                            "Percentage for ${category.name} adjusted to fit 100% total."
                        )
                    } else if (totalBudget <= 0f && enteredPercentage > 0f) {
                        // Optional: Toast if user enters % with 0 budget
                        // showToast(requireContext(), "Total budget is zero. Category amount is 0.")
                    }
                }
            }
            editText.addTextChangedListener(watcher)
            editText.setTag(watcherTag, watcher)

            editText.setOnFocusChangeListener { _, hasFocus ->
                if (isObserverUpdating || isPercentageFormatting || !hasLoadedInitialData) return@setOnFocusChangeListener

                val currentWatcher = editText.getTag(watcherTag) as? TextWatcher
                val currentValue = parsePercentageInput(editText.text.toString())

                if (!hasFocus) { // Lost focus - format to "X.Y%"
                    isPercentageFormatting = true // Lock for formatting
                    val vmPercent = budgetViewModel.categoryPercentages.value?.get(category)
                        ?: currentValue // Use VM value if available
                    val formattedText = formatPercentage(vmPercent)
                    safeSetEditText(
                        editText,
                        formattedText,
                        currentWatcher,
                        true,
                        isPercentageFormatting
                    ) { isPercentageFormatting = it }
                    isPercentageFormatting = false // Release lock

                } else { // Gained focus - format to raw number "X.Y" (remove %)
                    isPercentageFormatting = true // Lock for formatting
                    // Get current value, remove '%' if present for editing
                    val rawString = if (currentValue > 0) {
                        DecimalFormat(
                            "#0.0#",
                            DecimalFormatSymbols.getInstance(Locale.getDefault())
                        ).format(currentValue)
                    } else {
                        ""
                    }
                    // Format without '%', try to keep cursor position relative to the number part
                    safeSetEditText(
                        editText,
                        rawString,
                        currentWatcher,
                        false,
                        isPercentageFormatting
                    ) { isPercentageFormatting = it }
                    isPercentageFormatting = false // Release lock
                }
            }
        }
    }

    // --- Alarm Data Handling ---
    private fun observeAlarmData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    budgetViewModel.alarmThresholds.collect { thresholds ->
                        // Guard against internal formatting/observer loops
                        if (isCurrencyFormatting || isObserverUpdating) return@collect

                        isObserverUpdating = true // Indicate that update is from observer

                        // Iterate over all CategoryTypes to ensure all associated EditTexts are initialized.
                        // This handles cases where the 'thresholds' map is empty (first launch).
                        CategoryType.values().forEach { category ->
                            val etThreshold = categoryAlarmThresholdEditTexts[category]
                            if (etThreshold == null) return@forEach // Ensure EditText exists for the category

                            // Get the threshold value from the ViewModel's map for this category,
                            // or default to 0f if not found (for new/empty data).
                            val value = thresholds[category] ?: 0f

                            val formattedValue = formatCurrency(value)
                            val watcher =
                                etThreshold.getTag(R.id.alarm_threshold_watcher_tag_prefix + category.ordinal) as? TextWatcher

                            // Only update the EditText if its current text does not match the formatted value.
                            // This will ensure initial setup to "$0" or "$0.00" if it's currently blank or incorrect.
                            if (etThreshold.text.toString() != formattedValue) {
                                safeSetEditText(
                                    etThreshold,
                                    formattedValue,
                                    watcher,
                                    true, // Set cursor at end, suitable for initial display
                                    isCurrencyFormatting
                                ) { isCurrencyFormatting = it }
                            }
                        }

                        // 'hasLoadedInitialData' should now be reliably set by observeTotalBudget after the previous fix.
                        // This specific line might become less critical here, but doesn't hurt.
                        if (thresholds.isNotEmpty()) hasLoadedInitialData = true

                        isObserverUpdating = false // Release the lock
                    }
                }
                // The launch block for alarmEnabled remains the same:
                launch {
                    budgetViewModel.alarmEnabled.collect { enabledMap ->
                        if (isObserverUpdating) return@collect
                        isObserverUpdating = true
                        enabledMap.forEach { (category, isChecked) ->
                            categoryAlarmSwitches[category]?.let { checkBox ->
                                if (checkBox.isChecked != isChecked) checkBox.isChecked = isChecked
                            }
                        }
                        isObserverUpdating = false
                    }
                }
            }
        }
    }
    private fun setupCategoryAlarmInputsAndSwitches() {
        CategoryType.values().forEach { category ->
            val etThreshold = categoryAlarmThresholdEditTexts[category] ?: return@forEach
            val switchAlarm = categoryAlarmSwitches[category] ?: return@forEach

            // Observe category budget to update switch enabled state
            budgetViewModel.categoryBudgets.observe(viewLifecycleOwner) { categoryBudgets ->
                val categoryBudgetAmount = categoryBudgets[category] ?: 0f
                updateAlarmSwitchEnabledState(
                    switchAlarm,
                    categoryBudgetAmount,
                    category,
                    etThreshold
                )
            }

            val thresholdWatcherTag =
                R.id.alarm_threshold_watcher_tag_prefix + category.ordinal // Unique tag
            val thresholdWatcher = object : TextWatcher {
                private var currentText = ""

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Guards against internal formatting/observer updates/initial load
                    if (s.toString() == currentText || isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) {
                        return
                    }

                    isCurrencyFormatting = true // Lock for this EditText's formatting
                    etThreshold.removeTextChangedListener(this)

                    val userInput = s.toString()
                    val parsedValue =
                        parseCurrency(userInput).toFloat() // Parses "$1,234.56" to 1234.56f

                    val categoryBudget = budgetViewModel.categoryBudgets.value?.get(category) ?: 0f

                    // Clamp the threshold value to be non-negative and not exceed category budget
                    val clampedValue = parsedValue.coerceAtMost(categoryBudget.takeIf { it > 0 }
                        ?: Float.MAX_VALUE).coerceAtLeast(0f)

                    // Format the clamped value for display *immediately*
                    val displayStringToSet = formatCurrency(clampedValue)

                    // Update EditText visually
                    currentText = displayStringToSet
                    etThreshold.setText(displayStringToSet)
                    // Set selection to the end after formatting
                    etThreshold.setSelection(etThreshold.text.length.coerceAtMost(displayStringToSet.length))

                    etThreshold.addTextChangedListener(this)
                    isCurrencyFormatting = false // Release the lock

                    // Update ViewModel and persist changes
                    // This will trigger the observer, but it's guarded by isCurrencyFormatting
                    budgetViewModel.setAlarmThreshold(category, clampedValue)
                    budgetViewModel.persistAlarmThreshold(
                        category,
                        clampedValue.toDouble(),
                        switchAlarm.isChecked
                    )

                    // Provide feedback if clamping occurred
                    if (parsedValue > categoryBudget && categoryBudget > 0 && parsedValue != clampedValue) {
                        showToast(
                            requireContext(),
                            "Alarm threshold adjusted to not exceed category budget."
                        )
                    } else if (parsedValue < 0 && parsedValue != clampedValue) {
                        showToast(requireContext(), "Alarm threshold adjusted to be non-negative.")
                    }
                }
            }
            etThreshold.addTextChangedListener(thresholdWatcher)
            etThreshold.setTag(thresholdWatcherTag, thresholdWatcher)

            etThreshold.setOnFocusChangeListener { _, hasFocus ->
                if (isObserverUpdating || isCurrencyFormatting || !hasLoadedInitialData) return@setOnFocusChangeListener

                val currentWatcher = etThreshold.getTag(thresholdWatcherTag) as? TextWatcher
                val currentValue = parseCurrency(etThreshold.text.toString()).toFloat()


                if (!hasFocus) { // Lost focus - format to "$X,YYY.ZZ"
                    isCurrencyFormatting = true // Lock for formatting
                    val vmValue = budgetViewModel.alarmThresholdMap[category]
                        ?: currentValue // Use VM value if available
                    val formattedText = formatCurrency(vmValue.toFloat())
                    safeSetEditText(
                        etThreshold,
                        formattedText,
                        currentWatcher,
                        true,
                        isCurrencyFormatting
                    ) { isCurrencyFormatting = it }
                    isCurrencyFormatting = false // Release lock

                } else { // Gained focus - format to raw number "X000.YY" (remove currency/grouping)
                    isCurrencyFormatting = true // Lock for formatting
                    // Format without currency symbol and grouping for editing, if value > 0
                    val rawString = if (currentValue > 0) {
                        DecimalFormat(
                            "#0.##",
                            DecimalFormatSymbols.getInstance(Locale.getDefault())
                        ).format(currentValue)
                    } else {
                        ""
                    }
                    // Format to raw number, try to keep cursor position
                    safeSetEditText(
                        etThreshold,
                        rawString,
                        currentWatcher,
                        false,
                        isCurrencyFormatting
                    ) { isCurrencyFormatting = it }
                    isCurrencyFormatting = false // Release lock
                }
            }

            switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                if (isObserverUpdating || !hasLoadedInitialData) return@setOnCheckedChangeListener // Guard against observer loops/initial load

                val thresholdValue = parseCurrency(etThreshold.text.toString()).toFloat()
                val categoryBudget = budgetViewModel.categoryBudgets.value?.get(category) ?: 0f

                // Validation check: cannot activate alarm with 0 threshold or for 0 budget
                if (isChecked) { // If trying to check
                    if (thresholdValue <= 0f) {
                        isObserverUpdating = true; switchAlarm.isChecked =
                            false; isObserverUpdating = false
                        showToast(
                            requireContext(),
                            "Cannot activate alarm with zero/negative threshold."
                        )
                        return@setOnCheckedChangeListener
                    }
                    if (categoryBudget <= 0f) {
                        isObserverUpdating = true; switchAlarm.isChecked =
                            false; isObserverUpdating = false
                        showToast(
                            requireContext(),
                            "Cannot activate alarm for a category with no budget."
                        )
                        return@setOnCheckedChangeListener
                    }
                }

                // If unchecked, or valid checks passed for checking
                budgetViewModel.setAlarmEnabled(category, isChecked)
                // Persist change
                budgetViewModel.persistAlarmThreshold(
                    category,
                    thresholdValue.toDouble(),
                    isChecked
                )
            }
        }
    }

    private fun updateAlarmSwitchEnabledState(
        switch: MaterialCheckBox,
        categoryBudget: Float,
        category: CategoryType,
        etThreshold: EditText
    ) {
        val currentlyEnabled = switch.isEnabled
        val shouldBeEnabled = categoryBudget > 0f
        if (currentlyEnabled != shouldBeEnabled) {
            switch.isEnabled = shouldBeEnabled
            if (!shouldBeEnabled && switch.isChecked) {
                // If switch was checked but budget became 0, uncheck it and persist
                isObserverUpdating = true
                switch.isChecked = false
                isObserverUpdating = false
                val thresholdValue = parseCurrency(etThreshold.text.toString()).toFloat()
                budgetViewModel.setAlarmEnabled(category, false)
                budgetViewModel.persistAlarmThreshold(category, thresholdValue.toDouble(), false)
            }
        }
        // Also disable switch if threshold is 0, even if budget > 0 (handled in check change listener too)
        val thresholdValue = parseCurrency(etThreshold.text.toString()).toFloat()
        if (thresholdValue <= 0f && switch.isChecked && categoryBudget > 0f) {
            isObserverUpdating = true
            switch.isChecked = false
            isObserverUpdating = false
            budgetViewModel.setAlarmEnabled(category, false)
            budgetViewModel.persistAlarmThreshold(category, thresholdValue.toDouble(), false)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun observeOverviewState() {
        homeViewModel.overviewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* Handle loading */
                }

                is UiState.Success -> {
                    val overview = state.data
                    textViewIncome.text = formatCurrency(overview.totalIncome.toFloat())
                    textViewExpense.text = formatCurrency(overview.totalExpenses.toFloat())
                    textViewNetBalance.text = formatCurrency(overview.netBalance.toFloat())
                    // Mark initial data as loaded once overview is available as well
                    hasLoadedInitialData = true
                }

                is UiState.Error -> {
                    Log.e("PercentageBudgetFrag", "Error loading overview: ${state.message}")
                    showToast(requireContext(), "Error loading overview: ${state.message}")
                }
            }
        }
    }


    // --- Formatting & Parsing Utilities ---

    // Reusing the processAndFormatCurrencyInput from the value fragment - slightly adjusted
    private fun processAndFormatCurrencyInput(
        userInput: String,
        formatter: DecimalFormat,
        previousParsedValueForFallback: BigDecimal = BigDecimal.ZERO
    ): ProcessedCurrencyInput {
        val localDecimalSeparator = formatter.decimalFormatSymbols.decimalSeparator.toString()
        val localGroupingSeparator = formatter.decimalFormatSymbols.groupingSeparator.toString()

        var cleanString = userInput.replace(formatter.currency.symbol, "") // Remove currency symbol
        cleanString = cleanString.replace(localGroupingSeparator, "") // Remove grouping separator

        // Standardize decimal separator to '.' for BigDecimal parsing
        if (localDecimalSeparator != ".") {
            cleanString = cleanString.replace(localDecimalSeparator, ".")
        }

        // Handle potential multiple decimal points - keep only the last one
        val firstDecimalIndex = cleanString.indexOf('.')
        if (firstDecimalIndex != -1) {
            val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
            var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")
            // Limit fraction digits if needed, though BigDecimal parsing handles this
            cleanString = beforeDecimal + afterDecimal
        }


        val numberToParse = when {
            cleanString == "." -> "0." // Handle case where user types just a decimal point
            cleanString.isEmpty() -> "0" // Handle empty string as 0
            else -> cleanString
        }

        val parsedBigDecimal = try {
            BigDecimal(numberToParse).setScale(formatter.maximumFractionDigits, RoundingMode.DOWN)
        } catch (e: NumberFormatException) {
            previousParsedValueForFallback // Fallback on parse error
        }

        // Format for display using the formatter's locale rules
        var formattedDisplayString = formatter.format(parsedBigDecimal)

        // Preserve user-typed decimal point if necessary for a more natural input feel
        val userTypedDecimalAtEnd = userInput.endsWith(localDecimalSeparator)
        val formattedStringLostDecimal =
            !formattedDisplayString.contains(localDecimalSeparator) && (localDecimalSeparator == "." && !formattedDisplayString.contains(
                "."
            )) || (localDecimalSeparator == "," && !formattedDisplayString.contains(","))

        val isWholeNumberBasically = parsedBigDecimal.stripTrailingZeros().scale() <= 0


        if (userTypedDecimalAtEnd && formattedStringLostDecimal && isWholeNumberBasically) {
            // If the clean string ended with a decimal point and the formatted string didn't keep it, add it back
            if (numberToParse.endsWith(".")) {
                formattedDisplayString += localDecimalSeparator
            }
        } else if (numberToParse == "0." && !formattedDisplayString.contains(localDecimalSeparator)) {
            // Specific case for "0." input if formatter formats it as "$0" or similar without decimal
            formattedDisplayString = formatter.currency.symbol + "0" + localDecimalSeparator
        }


        return ProcessedCurrencyInput(formattedDisplayString, parsedBigDecimal, numberToParse)
    }


    // Refined percentage parsing logic
    private fun parsePercentageInput(input: String): Float {
        if (input.isBlank()) return 0f

        var cleanString = input.replace("%", "").trim()

        // Use the percentage formatter's decimal separator
        val localDecimalSeparator =
            percentageFormatter.decimalFormatSymbols.decimalSeparator.toString()
        // Standardize decimal separator to '.' for BigDecimal parsing
        if (localDecimalSeparator != ".") {
            cleanString = cleanString.replace(localDecimalSeparator, ".")
        }

        // Handle potential multiple decimal points - keep only the last one
        val firstDecimalIndex = cleanString.indexOf('.')
        if (firstDecimalIndex != -1) {
            val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
            var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")
            // Limit fraction digits based on the percentage formatter's precision
            if (afterDecimal.length > percentageFormatter.maximumFractionDigits) {
                afterDecimal = afterDecimal.substring(0, percentageFormatter.maximumFractionDigits)
            }
            cleanString = beforeDecimal + afterDecimal
        }

        val numberToParse = when {
            cleanString == "." -> "0." // Handle case where user types just a decimal point
            cleanString.isEmpty() -> "0" // Handle empty string as 0
            else -> cleanString
        }

        return try {
            // Parse as BigDecimal first for precision, then convert to Float
            BigDecimal(numberToParse)
                .setScale(
                    percentageFormatter.maximumFractionDigits,
                    RoundingMode.HALF_UP
                ) // Match formatter scale
                .toFloat()
        } catch (e: NumberFormatException) {
            0f // Return 0f on parse error
        }
    }

    private fun formatCurrency(value: Float): String {
        // Allow negative values for net balance if necessary.
        // Remove .coerceAtLeast(0f)
        val valueToFormat = if (value.isNaN()) 0f else value
        return currencyFormatter.format(BigDecimal(valueToFormat.toString()))
    }

    private fun formatPercentage(value: Float): String { // value is 0-100
        // Ensure non-negative and clamp to 100% for display
        val valueToFormat = if (value.isNaN()) 0f else value.coerceIn(0f, 100f)
        return percentageFormatter.format(BigDecimal(valueToFormat.toString()))
    }


    // Modified safeSetEditText to manage cursor position based on the flag
    private fun safeSetEditText(
        editText: EditText,
        text: String,
        watcher: TextWatcher?,
        setCursorAtEnd: Boolean,
        formattingFlag: Boolean, // Pass the relevant formatting flag
        setFormattingFlag: (Boolean) -> Unit // Pass a lambda to set the flag
    ) {
        val oldSelectionStart = editText.selectionStart
        setFormattingFlag(true) // Set the flag to indicate formatting is happening
        watcher?.let { editText.removeTextChangedListener(it) }

        if (editText.text.toString() != text) {
            editText.setText(text)
        }

        watcher?.let { editText.addTextChangedListener(it) }

        if (setCursorAtEnd) {
            editText.setSelection(text.length.coerceAtMost(editText.text.length))
        } else {
            // Try to restore original cursor position or set to where user was typing
            // Adjust position if text length changed significantly
            val newSelection =
                oldSelectionStart.coerceIn(0, text.length.coerceAtMost(editText.text.length))
            editText.setSelection(newSelection)
        }
        setFormattingFlag(false) // Reset the flag
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
}