package itson.appsmoviles.nest.ui.home.filter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.setUpTypesSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
class FilterMovementsFragment : DialogFragment() {

    private val sharedViewModel: SharedMovementsViewModel by activityViewModels()

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var tvByType: TextView
    private lateinit var tvByCategory: TextView
    private lateinit var tvByDate: TextView
    private lateinit var spinnerCategories: Spinner
    private lateinit var spinnerTypes: Spinner
    private lateinit var btnApplyFilters: Button
    private lateinit var btnClearFilters: ImageButton
    private var startSelectedTimestamp: Long? = null
    private var endSelectedTimestamp: Long? = null

    private val movementTypes = listOf("Incomes", "Expenses")

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_movements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setUpSpinner(requireContext(), spinnerCategories)
        setUpTypesSpinner(requireContext(), spinnerTypes)
        restoreFilterState()
        setupActionListeners()
    }

    private fun initializeViews(view: View) {
        btnStartDate = view.findViewById(R.id.btn_start_date_filter)
        btnEndDate = view.findViewById(R.id.btn_end_date_filter)
        spinnerCategories = view.findViewById(R.id.spinner_categories_filter)
        spinnerTypes = view.findViewById(R.id.spinner_types_filter)
        btnApplyFilters = view.findViewById(R.id.btn_apply_filters)
        btnClearFilters = view.findViewById(R.id.btn_clear_filters)
        tvByType = view.findViewById(R.id.tv_by_type_filter)
        tvByCategory = view.findViewById(R.id.tv_by_category_filter)
        tvByDate = view.findViewById(R.id.tv_by_date_filter)
    }


    private fun setupActionListeners() {
        setupDateButtonListeners()
        setupFilterButtonListeners()
        setupSpinnerSelectionListeners()
    }

    private fun setupDateButtonListeners() {
        btnStartDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                maxTimestamp = endSelectedTimestamp ?: System.currentTimeMillis(),
                onDateSelected = { timestampMillis ->
                    startSelectedTimestamp = timestampMillis
                    btnStartDate.text = formatDateShortForm(timestampMillis)
                }
            )
        }
        btnEndDate.setOnClickListener {

            showDatePicker(
                context = requireContext(),
                minTimestamp = startSelectedTimestamp ?: 0,
                onDateSelected = { timestampMillis ->
                    endSelectedTimestamp = timestampMillis
                    btnEndDate.text = formatDateShortForm(timestampMillis)
                }
            )
        }
    }


    private fun setupFilterButtonListeners() {
        btnApplyFilters.setOnClickListener {
            applyFilters()
            dismiss()
        }
        btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupSpinnerSelectionListeners() {
        spinnerTypes.onItemSelectedListener = createTypeSpinnerListener()
        spinnerCategories.onItemSelectedListener = createCategorySpinnerListener()
    }

    private fun createTypeSpinnerListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                handleTypeSelection(parent?.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                enableCategorySpinner()
            }
        }
    }

    private fun createCategorySpinnerListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                handleCategorySelection(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun handleTypeSelection(selectedType: String) {
        if (selectedType == movementTypes[0]) { // "Incomes"
            disableCategorySpinner()
        } else {
            enableCategorySpinner()
        }
    }

    private fun handleCategorySelection(position: Int) {
        if (position > 0) {
            val typesAdapter = spinnerTypes.adapter as ArrayAdapter<String>
            val expensePosition = typesAdapter.getPosition(movementTypes[1])

            if (expensePosition >= 0 && spinnerTypes.selectedItemPosition != expensePosition) {
                spinnerTypes.setSelection(expensePosition)
            }
        }
    }

    private fun enableCategorySpinner() {
        tvByCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        spinnerCategories.isEnabled = true
        spinnerCategories.alpha = 1.0f
    }

    private fun disableCategorySpinner() {
        tvByCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_hint))
        spinnerCategories.isEnabled = false
        spinnerCategories.alpha = 0.5f
        spinnerCategories.setSelection(0)
    }


    private fun applyFilters() {
        val selectedType = getSelectedMovementType()
        val selectedCategory = getSelectedCategory(selectedType)

        val criteria = FilterCriteria(
            startDate = startSelectedTimestamp,
            endDate = endSelectedTimestamp,
            movementType = selectedType,
            category = selectedCategory
        )
        sharedViewModel.updateFilters(criteria)
    }

    private fun getSelectedMovementType(): String? {
        val selectedTypePosition = spinnerTypes.selectedItemPosition
        return if (selectedTypePosition > 0) {
            spinnerTypes.getItemAtPosition(selectedTypePosition).toString()
        } else {
            null
        }
    }

    private fun getSelectedCategory(selectedType: String?): CategoryType? {
        val selectedCategoryPosition = spinnerCategories.selectedItemPosition
        if (selectedCategoryPosition > 0 && selectedType == movementTypes[1]) {
            val selectedCategoryName =
                spinnerCategories.getItemAtPosition(selectedCategoryPosition).toString()
            return CategoryType.fromDisplayName(selectedCategoryName)
        }
        return null
    }

    private fun clearFilters() {
        resetDateFilters()
        resetSpinnerSelections()
        enableCategorySpinner()
        sharedViewModel.clearFilters()
    }

    private fun resetDateFilters() {
        startSelectedTimestamp = null
        endSelectedTimestamp = null
        btnStartDate.text = ""
        btnEndDate.text = ""
        btnStartDate.hint = getString(R.string.start_date)
        btnEndDate.hint = getString(R.string.end_date)
    }

    private fun resetSpinnerSelections() {
        spinnerCategories.setSelection(0)
        spinnerTypes.setSelection(0)
    }

    private fun restoreFilterState() {
        val currentCriteria = sharedViewModel.filterCriteria.value ?: FilterCriteria()

        restoreDateFilters(currentCriteria)
        restoreTypeSpinner(currentCriteria)
        restoreCategorySpinnerAndState(currentCriteria)
    }

    private fun restoreDateFilters(criteria: FilterCriteria) {
        criteria.startDate?.let {
            startSelectedTimestamp = it
            btnStartDate.text = formatDateShortForm(it)
        } ?: run {
            startSelectedTimestamp = null
            btnStartDate.text = ""
            btnStartDate.hint = getString(R.string.start_date)
        }

        criteria.endDate?.let {
            endSelectedTimestamp = it
            val displayDate = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            btnEndDate.text = formatDateShortForm(displayDate)
        } ?: run {
            endSelectedTimestamp = null
            btnEndDate.text = ""
            btnEndDate.hint = getString(R.string.end_date)
        }
    }

    private fun restoreTypeSpinner(criteria: FilterCriteria) {
        val typeAdapter = spinnerTypes.adapter as ArrayAdapter<String>
        var typePosition = 0
        criteria.movementType?.let { typeName ->
            val foundPosition = typeAdapter.getPosition(typeName)
            if (foundPosition > 0) {
                typePosition = foundPosition
            }
        }
        spinnerTypes.setSelection(typePosition)
    }

    private fun restoreCategorySpinnerAndState(criteria: FilterCriteria) {
        val typeAdapter = spinnerTypes.adapter as ArrayAdapter<String>
        val selectedTypeNameAfterRestore = if (spinnerTypes.selectedItemPosition > 0) {
            typeAdapter.getItem(spinnerTypes.selectedItemPosition)
        } else {
            null
        }

        val isCategoryEnabled = selectedTypeNameAfterRestore == movementTypes[1] // "Expenses"

        if (isCategoryEnabled) {
            enableCategorySpinner()
            criteria.category?.let { category ->
                restoreCategorySelection(category)
            } ?: spinnerCategories.setSelection(0)
        } else {
            disableCategorySpinner()
            spinnerCategories.setSelection(0)
        }
    }

    private fun restoreCategorySelection(categoryToRestore: CategoryType) {
        val categoryAdapter = spinnerCategories.adapter as? ArrayAdapter<String>
        var categoryPositionToSelect = 0

        categoryAdapter?.let { adapter ->
            val targetDisplayName = categoryToRestore.displayName
            for (i in 1 until adapter.count) {
                if (adapter.getItem(i) == targetDisplayName) {
                    categoryPositionToSelect = i
                    break
                }
            }
        }
        spinnerCategories.setSelection(categoryPositionToSelect)
    }
}