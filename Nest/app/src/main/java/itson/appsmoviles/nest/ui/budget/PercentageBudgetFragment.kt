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

    private val budgetViewModel: BudgetViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                    val sharedVM = ViewModelProvider(requireActivity())[SharedMovementsViewModel::class.java]
                    return BudgetViewModel(requireActivity().application, sharedVM) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

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
            isGroupingUsed = true
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

    private var isCurrencyFormatting = false
    private var isPercentageFormatting = false
    private var hasLoadedInitialData = false
    private var isObserverUpdating =
        false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupTotalBudgetInput()
        observeTotalBudget()


        observeCategoryPercentages()
        observeAlarmData()
        setupCategoryPercentageInputs()
        setupCategoryAlarmInputsAndSwitches()

        observeOverviewState()

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


    private fun setupTotalBudgetInput() {
        val totalBudgetWatcher = object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == currentText || isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) {
                     return
                }

                isCurrencyFormatting = true
                editTextTotalBudget.removeTextChangedListener(this)

                val userInput = s.toString()

                val fallbackValue =
                    budgetViewModel.totalBudget.value?.let { BigDecimal(it.toString()) }
                        ?: BigDecimal.ZERO
                val processedInput =
                    processAndFormatCurrencyInput(userInput, currencyFormatter, fallbackValue)


                budgetViewModel.setTotalBudget(processedInput.parsedValue.toFloat())


                currentText = processedInput.displayString
                editTextTotalBudget.setText(currentText)

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

        editTextTotalBudget.setTag(R.id.monthly_budget_watcher_tag, totalBudgetWatcher)

        editTextTotalBudget.setOnFocusChangeListener { _, hasFocus ->
            if (isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) return@setOnFocusChangeListener
            val watcher =
                editTextTotalBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher

            if (!hasFocus) {

                val vmValue = budgetViewModel.totalBudget.value ?: 0f
                safeSetEditText(
                    editTextTotalBudget,
                    formatCurrency(vmValue),
                    watcher,
                    true,
                    isCurrencyFormatting
                ) { isCurrencyFormatting = it }
            } else {
                val currentValue = budgetViewModel.totalBudget.value ?: parseCurrency(
                    editTextTotalBudget.text.toString()
                ).toFloat()

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

            if (isCurrencyFormatting || isObserverUpdating) return@observe

            isObserverUpdating = true
            val formattedTotal = formatCurrency(total)
            if (editTextTotalBudget.text.toString() != formattedTotal) {
                val watcher =
                    editTextTotalBudget.getTag(R.id.monthly_budget_watcher_tag) as? TextWatcher

                safeSetEditText(
                    editTextTotalBudget,
                    formattedTotal,
                    watcher,
                    true,
                    isCurrencyFormatting
                ) { isCurrencyFormatting = it }
            }


            if (budgetViewModel.totalBudget.value != null) hasLoadedInitialData = true

            isObserverUpdating = false
        }
    }



    private fun observeCategoryPercentages() {
        budgetViewModel.categoryPercentages.observe(viewLifecycleOwner) { percentagesMap ->

            if (isPercentageFormatting || isObserverUpdating) return@observe

            isObserverUpdating = true
            percentagesMap.forEach { (category, percent) ->
                categoryPercentageEditTexts[category]?.let { editText ->
                    val formattedPercentage = formatPercentage(percent)

                    if (editText.text.toString() != formattedPercentage) {
                        val watcher =
                            editText.getTag(R.id.percentage_watcher_tag_prefix + category.ordinal) as? TextWatcher

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

            if (percentagesMap.isNotEmpty()) hasLoadedInitialData = true

            isObserverUpdating = false
        }
    }

    private fun setupCategoryPercentageInputs() {
        categoryPercentageEditTexts.forEach { (category, editText) ->
            val watcherTag = R.id.percentage_watcher_tag_prefix + category.ordinal
            val watcher = object : TextWatcher {
                private var currentText = ""
                private var isSelfUpdate = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isSelfUpdate || s.toString() == currentText || isPercentageFormatting || isObserverUpdating || !hasLoadedInitialData) {
                        return
                    }

                    isPercentageFormatting = true
                    editText.removeTextChangedListener(this)

                    val userInput = s.toString()


                    if (!userInput.endsWith("%") || userInput.length > 1) {
                        val cleanInput = userInput.replace("%", "")
                        val enteredPercentage = if (cleanInput.isEmpty()) 0f else parsePercentageInput(cleanInput)


                        val totalBudget = budgetViewModel.totalBudget.value ?: 0f
                        val otherPercentagesSum = budgetViewModel.categoryPercentages.value
                            ?.filterKeys { it != category }
                            ?.values?.sum() ?: 0f

                        val maxAllowedPercentage = (100f - otherPercentagesSum).coerceAtLeast(0f)
                        var finalValidPercentage = enteredPercentage.coerceAtMost(maxAllowedPercentage)
                        finalValidPercentage = finalValidPercentage.toBigDecimal()
                            .setScale(percentageFormatter.maximumFractionDigits, RoundingMode.HALF_UP)
                            .toFloat()

                        val actualAmount = if (totalBudget > 0f) {
                            (totalBudget * finalValidPercentage / 100f)
                                .toBigDecimal()
                                .setScale(currencyFormatter.maximumFractionDigits, RoundingMode.HALF_UP)
                                .toFloat()
                        } else {
                            0f
                        }

                        budgetViewModel.setCategoryBudget(
                            category, actualAmount,
                            budgetViewModel.alarmThresholdMap[category] ?: 0f,
                            budgetViewModel.alarmEnabledMap[category] ?: false
                        )

                        val displayString = formatPercentage(finalValidPercentage)
                        currentText = displayString

                        isSelfUpdate = true
                        editText.setText(displayString)
                        editText.setSelection(displayString.length - 1)
                        isSelfUpdate = false
                    }

                    editText.addTextChangedListener(this)
                    isPercentageFormatting = false
                }
            }

            editText.addTextChangedListener(watcher)
            editText.setTag(watcherTag, watcher)


            editText.setOnFocusChangeListener { _, hasFocus ->
                if (isObserverUpdating || isPercentageFormatting || !hasLoadedInitialData) return@setOnFocusChangeListener

                val currentWatcher = editText.getTag(watcherTag) as? TextWatcher
                val currentValue = parsePercentageInput(editText.text.toString())

                if (!hasFocus) {
                    isPercentageFormatting = true
                    val formattedText = formatPercentage(currentValue)
                    safeSetEditText(
                        editText,
                        formattedText,
                        currentWatcher,
                        true,
                        isPercentageFormatting
                    ) { isPercentageFormatting = it }
                } else {

                    editText.setSelection(editText.text.length - 1)
                }
            }
        }
    }


    private fun observeAlarmData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    budgetViewModel.alarmThresholds.collect { thresholds ->

                        if (isCurrencyFormatting || isObserverUpdating) return@collect

                        isObserverUpdating = true


                        CategoryType.values().forEach { category ->
                            val etThreshold = categoryAlarmThresholdEditTexts[category]
                            if (etThreshold == null) return@forEach
                            val value = thresholds[category] ?: 0f

                            val formattedValue = formatCurrency(value)
                            val watcher =
                                etThreshold.getTag(R.id.alarm_threshold_watcher_tag_prefix + category.ordinal) as? TextWatcher


                            if (etThreshold.text.toString() != formattedValue) {
                                safeSetEditText(
                                    etThreshold,
                                    formattedValue,
                                    watcher,
                                    true,
                                    isCurrencyFormatting
                                ) { isCurrencyFormatting = it }
                            }
                        }

                        if (thresholds.isNotEmpty()) hasLoadedInitialData = true

                        isObserverUpdating = false
                    }
                }

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

                    if (s.toString() == currentText || isCurrencyFormatting || isObserverUpdating || !hasLoadedInitialData) {
                        return
                    }

                    isCurrencyFormatting = true
                    etThreshold.removeTextChangedListener(this)

                    val userInput = s.toString()
                    val parsedValue =
                        parseCurrency(userInput).toFloat()

                    val categoryBudget = budgetViewModel.categoryBudgets.value?.get(category) ?: 0f


                    val clampedValue = parsedValue.coerceAtMost(categoryBudget.takeIf { it > 0 }
                        ?: Float.MAX_VALUE).coerceAtLeast(0f)


                    val displayStringToSet = formatCurrency(clampedValue)


                    currentText = displayStringToSet
                    etThreshold.setText(displayStringToSet)

                    etThreshold.setSelection(etThreshold.text.length.coerceAtMost(displayStringToSet.length))

                    etThreshold.addTextChangedListener(this)
                    isCurrencyFormatting = false


                    budgetViewModel.setAlarmThreshold(category, clampedValue)
                    budgetViewModel.persistAlarmThreshold(
                        category,
                        clampedValue.toDouble(),
                        switchAlarm.isChecked
                    )


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


                if (!hasFocus) {
                    isCurrencyFormatting = true
                    val vmValue = budgetViewModel.alarmThresholdMap[category]
                        ?: currentValue
                    val formattedText = formatCurrency(vmValue.toFloat())
                    safeSetEditText(
                        etThreshold,
                        formattedText,
                        currentWatcher,
                        true,
                        isCurrencyFormatting
                    ) { isCurrencyFormatting = it }
                    isCurrencyFormatting = false

                } else {
                    isCurrencyFormatting = true

                    val rawString = if (currentValue > 0) {
                        DecimalFormat(
                            "#0.##",
                            DecimalFormatSymbols.getInstance(Locale.getDefault())
                        ).format(currentValue)
                    } else {
                        ""
                    }

                    safeSetEditText(
                        etThreshold,
                        rawString,
                        currentWatcher,
                        false,
                        isCurrencyFormatting
                    ) { isCurrencyFormatting = it }
                    isCurrencyFormatting = false
                }
            }

            switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                if (isObserverUpdating || !hasLoadedInitialData) return@setOnCheckedChangeListener // Guard against observer loops/initial load

                val thresholdValue = parseCurrency(etThreshold.text.toString()).toFloat()
                val categoryBudget = budgetViewModel.categoryBudgets.value?.get(category) ?: 0f


                if (isChecked) {
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


                budgetViewModel.setAlarmEnabled(category, isChecked)

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

                isObserverUpdating = true
                switch.isChecked = false
                isObserverUpdating = false
                val thresholdValue = parseCurrency(etThreshold.text.toString()).toFloat()
                budgetViewModel.setAlarmEnabled(category, false)
                budgetViewModel.persistAlarmThreshold(category, thresholdValue.toDouble(), false)
            }
        }

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
                is UiState.Loading -> {
                }

                is UiState.Success -> {
                    val overview = state.data
                    textViewIncome.text = formatCurrency(overview.totalIncome.toFloat())
                    textViewExpense.text = formatCurrency(overview.totalExpenses.toFloat())
                    textViewNetBalance.text = formatCurrency(overview.netBalance.toFloat())

                    hasLoadedInitialData = true
                }

                is UiState.Error -> {
                    Log.e("PercentageBudgetFrag", "Error loading overview: ${state.message}")
                    showToast(requireContext(), "Error loading overview: ${state.message}")
                }
            }
        }
    }



    private fun processAndFormatCurrencyInput(
        userInput: String,
        formatter: DecimalFormat,
        previousParsedValueForFallback: BigDecimal = BigDecimal.ZERO
    ): ProcessedCurrencyInput {
        val localDecimalSeparator = formatter.decimalFormatSymbols.decimalSeparator.toString()
        val localGroupingSeparator = formatter.decimalFormatSymbols.groupingSeparator.toString()

        var cleanString = userInput.replace(formatter.currency.symbol, "")
        cleanString = cleanString.replace(localGroupingSeparator, "")


        if (localDecimalSeparator != ".") {
            cleanString = cleanString.replace(localDecimalSeparator, ".")
        }


        val firstDecimalIndex = cleanString.indexOf('.')
        if (firstDecimalIndex != -1) {
            val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
            var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")

            cleanString = beforeDecimal + afterDecimal
        }


        val numberToParse = when {
            cleanString == "." -> "0."
            cleanString.isEmpty() -> "0"
            else -> cleanString
        }

        val parsedBigDecimal = try {
            BigDecimal(numberToParse).setScale(formatter.maximumFractionDigits, RoundingMode.DOWN)
        } catch (e: NumberFormatException) {
            previousParsedValueForFallback
        }


        var formattedDisplayString = formatter.format(parsedBigDecimal)


        val userTypedDecimalAtEnd = userInput.endsWith(localDecimalSeparator)
        val formattedStringLostDecimal =
            !formattedDisplayString.contains(localDecimalSeparator) && (localDecimalSeparator == "." && !formattedDisplayString.contains(
                "."
            )) || (localDecimalSeparator == "," && !formattedDisplayString.contains(","))

        val isWholeNumberBasically = parsedBigDecimal.stripTrailingZeros().scale() <= 0


        if (userTypedDecimalAtEnd && formattedStringLostDecimal && isWholeNumberBasically) {

            if (numberToParse.endsWith(".")) {
                formattedDisplayString += localDecimalSeparator
            }
        } else if (numberToParse == "0." && !formattedDisplayString.contains(localDecimalSeparator)) {

            formattedDisplayString = formatter.currency.symbol + "0" + localDecimalSeparator
        }


        return ProcessedCurrencyInput(formattedDisplayString, parsedBigDecimal, numberToParse)
    }



    private fun parsePercentageInput(input: String): Float {
        if (input.isBlank()) return 0f

        var cleanString = input.replace("%", "").trim()


        val localDecimalSeparator =
            percentageFormatter.decimalFormatSymbols.decimalSeparator.toString()

        if (localDecimalSeparator != ".") {
            cleanString = cleanString.replace(localDecimalSeparator, ".")
        }


        val firstDecimalIndex = cleanString.indexOf('.')
        if (firstDecimalIndex != -1) {
            val beforeDecimal = cleanString.substring(0, firstDecimalIndex + 1)
            var afterDecimal = cleanString.substring(firstDecimalIndex + 1).replace(".", "")

            if (afterDecimal.length > percentageFormatter.maximumFractionDigits) {
                afterDecimal = afterDecimal.substring(0, percentageFormatter.maximumFractionDigits)
            }
            cleanString = beforeDecimal + afterDecimal
        }

        val numberToParse = when {
            cleanString == "." -> "0."
            cleanString.isEmpty() -> "0"
            else -> cleanString
        }

        return try {

            BigDecimal(numberToParse)
                .setScale(
                    percentageFormatter.maximumFractionDigits,
                    RoundingMode.HALF_UP
                )
                .toFloat()
        } catch (e: NumberFormatException) {
            0f
        }
    }

    private fun formatCurrency(value: Float): String {
        val valueToFormat = if (value.isNaN()) 0f else value
        return currencyFormatter.format(BigDecimal(valueToFormat.toString()))
    }

    private fun formatPercentage(value: Float): String {
        val valueToFormat = if (value.isNaN()) 0f else value.coerceIn(0f, 100f)
        return percentageFormatter.format(BigDecimal(valueToFormat.toString()))
    }


    private fun safeSetEditText(
        editText: EditText,
        text: String,
        watcher: TextWatcher?,
        setCursorAtEnd: Boolean,
        formattingFlag: Boolean,
        setFormattingFlag: (Boolean) -> Unit
    ) {
        setFormattingFlag(true)
        watcher?.let { editText.removeTextChangedListener(it) }

        if (editText.text.toString() != text) {
            editText.setText(text)
        }

        watcher?.let { editText.addTextChangedListener(it) }

        when {
            setCursorAtEnd -> editText.setSelection(text.length)
            editText.hasFocus() -> editText.selectAll()
            else -> editText.setSelection(text.length)
        }

        setFormattingFlag(false)
    }

    private fun parseCurrency(input: String): BigDecimal {
        if (input.isBlank()) return BigDecimal.ZERO
        return try {

            val cleanString = input.replace("$", "").replace(currencyFormatter.decimalFormatSymbols.groupingSeparator.toString(), "")

            val parsableString = cleanString.replace(currencyFormatter.decimalFormatSymbols.decimalSeparator.toString(), ".")
            BigDecimal(parsableString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }
}