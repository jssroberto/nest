package itson.appsmoviles.nest.ui.expenses

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.data.model.Category
import itson.appsmoviles.nest.ui.add.expense.ExpensesViewModel
import itson.appsmoviles.nest.ui.expenses.drawable.ExpensesDrawable
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable
import itson.appsmoviles.nest.ui.util.clearFilters
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class ExpensesFragment : Fragment() {

    private val expenseRepository = ExpenseRepository()
    private val categories = arrayListOf(
        Category(CategoryType.LIVING, 0.0f, R.color.category_living, 0.0f),
        Category(CategoryType.RECREATION, 0.0f, R.color.category_recreation, 0.0f),
        Category(CategoryType.TRANSPORT, 0.0f, R.color.category_transport, 0.0f),
        Category(CategoryType.FOOD, 0.0f, R.color.category_food, 0.0f),
        Category(CategoryType.HEALTH, 0.0f, R.color.category_health, 0.0f),
        Category(CategoryType.OTHER, 0.0f, R.color.category_other, 0.0f)
    )

    private var selectedCategoryName: String? = null
    private lateinit var pieChartDrawable: PieChartDrawable
    private val categoryTextViews = mutableListOf<TextView>()
    private var startSelectedTimestamp: Long? = null
    private var endSelectedTimestamp: Long? = null
    private val viewModel: ExpensesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpensesViewModel(ExpenseRepository()) as T
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_expenses, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startDateButton: Button = view.findViewById(R.id.btn_date_income)
        val endDateButton: Button = view.findViewById(R.id.btn_end_date)
        val totalExpensesTextView = view.findViewById<TextView>(R.id.totalExpenses)
        val clean: ImageButton = view.findViewById(R.id.btn_delete_filters)

        setUpSpinner(requireContext(), view.findViewById(R.id.spinner_categories_income))

        configurePieChart(view)

        startDateButton.setOnClickListener { showDatePicker(
                context = requireContext(),
                onDateSelected = { timestampMillis ->
                    startSelectedTimestamp = timestampMillis
                    startDateButton.text = formatDateShortForm(timestampMillis)
                }
        ) }
        endDateButton.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                onDateSelected = { timestampMillis ->
                    endSelectedTimestamp = timestampMillis
                    endDateButton.text = formatDateShortForm(timestampMillis)
                }
            )
        }

        clean.setOnClickListener {
            clearFilters(
                view.findViewById(R.id.spinner_categories_income),
                startDateButton,
                endDateButton,
                requireContext()
            )
        }

        view.findViewById<Button>(R.id.btn_filter)?.setOnClickListener {
            filterAndLoadExpenses()
        }

        totalExpensesTextView.text = "$${calculateTotal()}"

        setupCategoryTextViews(view)


        viewLifecycleOwner.lifecycleScope.launch {
            loadExpenses()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterAndLoadExpenses() {
        val spinner = requireView().findViewById<Spinner>(R.id.spinner_categories_income)
        val selectedCategoryText = spinner.selectedItem?.toString()
        val selectedCategoryName = if (
            selectedCategoryText.isNullOrBlank() ||
            selectedCategoryText.equals("Select a category", ignoreCase = true)
        ) null else selectedCategoryText

        viewModel.getFilteredExpenses(startSelectedTimestamp, endSelectedTimestamp, selectedCategoryName)
            .observe(viewLifecycleOwner) { expenses ->
                updateChartWithExpenses(expenses)
            }
    }

    private fun updateChartWithExpenses(expenses: List<Expense>) {

        categories.forEach { it.total = 0.0f }

        val groupedExpenses = expenses.groupBy { it.category.displayName }

        groupedExpenses.forEach { (name, expensesList) ->
            val totalAmount = expensesList.sumOf { it.amount.toDouble() }.toFloat()


            categories.find { it.type.displayName == name }?.let {
                it.total = totalAmount
            }
        }

        updatePieChart()
        updateTotal()
    }



    private fun setupCategoryTextViews(view: View) {
        val foodTextView = view.findViewById<TextView>(R.id.foodTextView)
        val transportTextView = view.findViewById<TextView>(R.id.transportTextView)
        val healthTextView = view.findViewById<TextView>(R.id.healthTextView)
        val homeTextView = view.findViewById<TextView>(R.id.homeTextView)
        val recreationTextView = view.findViewById<TextView>(R.id.recreationTextView)
        val othersTextView = view.findViewById<TextView>(R.id.othersTextView)

        listOf(
            foodTextView to "Food",
            transportTextView to "Transport",
            healthTextView to "Health",
            homeTextView to "Home",
            recreationTextView to "Recreation",
            othersTextView to "Others"
        ).forEach { (textView, text) ->
            textView.tag = text
            categoryTextViews.add(textView)


            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))


            setupCategoryClick(textView, text)
        }
    }

    private fun setupCategoryClick(textView: TextView, categoryName: String) {
        textView.setOnClickListener {
            if (selectedCategoryName == categoryName) {

                selectedCategoryName = null
                clearAllCategorySelections()
                pieChartDrawable.selectedCategory = null
                requireView().findViewById<View>(R.id.graph).invalidate()
            } else {

                selectedCategoryName = categoryName
                highlightSelectedCategory(textView, categoryName)
                pieChartDrawable.selectedCategory = categories.find { it.type.displayName == categoryName }
                requireView().findViewById<View>(R.id.graph).invalidate()
            }
        }
    }


    private fun clearAllCategorySelections() {
        val somethingSelected = selectedCategoryName != null

        categoryTextViews.forEach { textView ->
            val isSelected = textView.tag == selectedCategoryName
            val color = if (somethingSelected && !isSelected) {
                R.color.gray
            } else {
                R.color.black
            }

            textView.setTextColor(ContextCompat.getColor(requireContext(), color))
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            textView.text = textView.tag?.toString() ?: ""
        }
    }



    private fun highlightSelectedCategory(selectedTextView: TextView, categoryName: String) {
        clearAllCategorySelections()


        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))



        val percentage = categories.find { it.type.displayName == categoryName }?.percentage ?: 0.0f
        selectedTextView.text = "$categoryName  ${"%.1f".format(percentage)}%"

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurePieChart(view: View) {
        val graphView = view.findViewById<View>(R.id.graph)
        pieChartDrawable = PieChartDrawable(requireContext(), categories)
        graphView.background = pieChartDrawable

        graphView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y
                val clickedCategory = pieChartDrawable.getCategoryFromTouch(x, y)

                if (clickedCategory != null) {
                    if (pieChartDrawable.selectedCategory == clickedCategory) {
                        // Ya estaba seleccionada → deselecciona
                        selectedCategoryName = null
                        pieChartDrawable.selectedCategory = null
                        clearAllCategorySelections()
                    } else {
                        // Selecciona nueva categoría
                        selectedCategoryName = clickedCategory.type.displayName
                        pieChartDrawable.selectedCategory = clickedCategory
                        categoryTextViews.find { it.tag == clickedCategory.type.displayName }?.let {
                            highlightSelectedCategory(it, clickedCategory.type.displayName)
                        }
                    }
                } else {
                    // Tocó fuera del gráfico
                    selectedCategoryName = null
                    pieChartDrawable.selectedCategory = null
                    clearAllCategorySelections()
                }

                graphView.invalidate()
            }
            true
        }
    }

    private fun calculateTotal(): Float {
        return categories.sumOf { it.total.toDouble() }.toFloat()
    }


    private suspend fun loadExpenses() {
        val startDate = view?.findViewById<Button>(R.id.btn_date_income)?.text?.toString()
            ?.takeIf { it.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")) }

        val endDate = view?.findViewById<Button>(R.id.btn_end_date)?.text?.toString()
            ?.takeIf { it.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")) }

        val spinner = view?.findViewById<Spinner>(R.id.spinner_categories_income)
        val selectedCategory = if (spinner?.selectedItemPosition != 0) {
            spinner?.selectedItem.toString().takeIf { it != "All categories" }
        } else {
            null
        }

        val expenses = expenseRepository.getExpensesFiltered(
            category = selectedCategory,
            startDate = startDate,
            endDate = endDate
        )

        updateChartWithExpenses(expenses)

        calculateProgressBars(
            requireView(),
            expenses.filter { it.category.displayName == "Food" }.sumOf { it.amount.toDouble() }.toFloat(), 75f,
            expenses.filter { it.category.displayName == "Transport" }.sumOf { it.amount.toDouble() }.toFloat(), 1f,
            expenses.filter { it.category.displayName == "Health" }.sumOf { it.amount.toDouble() }.toFloat(), 50f,
            expenses.filter { it.category.displayName == "Others" }.sumOf { it.amount.toDouble() }.toFloat(), 50f,
            expenses.filter { it.category.displayName == "Home" }.sumOf { it.amount.toDouble() }.toFloat(), 41f,
            expenses.filter { it.category.displayName == "Recreation" }.sumOf { it.amount.toDouble() }.toFloat(), 20f
        )
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun updatePieChart() {
        val graphView = view?.findViewById<View>(R.id.graph)
        pieChartDrawable = PieChartDrawable(requireContext(), categories)
        graphView?.background = pieChartDrawable


    }


    private fun updateTotal() {
        val totalExpensesTextView = view?.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView?.text = "$${calculateTotal()}"
    }

    private fun calculateProgressBars(
        view: View,
        totalFood: Float, currentFood: Float,
        totalTransport: Float, currentTransport: Float,
        totalHealth: Float, currentHealth: Float,
        totalOthers: Float, currentOthers: Float,
        totalHome: Float, currentHome: Float,
        totalRecreation: Float, currentRecreation: Float
    ) {
        calculateProgress(view.findViewById(R.id.foodBudget), totalFood, currentFood)
        calculateProgress(view.findViewById(R.id.transportBudget), totalTransport, currentTransport)
        calculateProgress(view.findViewById(R.id.budgetHealth), totalHealth, currentHealth)
        calculateProgress(view.findViewById(R.id.budgetOthers), totalOthers, currentOthers)
        calculateProgress(view.findViewById(R.id.budgetHome), totalHome, currentHome)
        calculateProgress(view.findViewById(R.id.budgetRecreation), totalRecreation, currentRecreation)
    }

    private fun calculateProgress(view: View, total: Float, current: Float) {
        val progressColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.txt_income)
        val progressDrawable = ExpensesDrawable(total, current, progressColor, backgroundColor)
        view.background = progressDrawable
    }
}