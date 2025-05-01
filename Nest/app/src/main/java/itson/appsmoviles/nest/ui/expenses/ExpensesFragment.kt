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
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.data.model.Category
import itson.appsmoviles.nest.ui.expenses.drawable.ExpensesDrawable
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class ExpensesFragment : Fragment() {

    private val expenseRepository = ExpenseRepository()
    private val categories = arrayListOf(
        Category("Health", 0.0f, R.color.lightest_blue, 0.0f),
        Category("Home", 0.0f, R.color.lighter_blue, 0.0f),
        Category("Food", 0.0f, R.color.light_blue, 0.0f),
        Category("Recreation", 0.0f, R.color.dark_blue, 0.0f),
        Category("Transport", 0.0f, R.color.darker_blue, 0.0f),
        Category("Others", 0.0f, R.color.blue, 0.0f)
    )

    private var selectedCategoryName: String? = null
    private lateinit var pieChartDrawable: PieChartDrawable
    private val categoryTextViews = mutableListOf<TextView>()

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

        configureSpinner(view)
        configurePieChart(view)

        startDateButton.setOnClickListener { showDatePicker(startDateButton) }
        endDateButton.setOnClickListener { showDatePicker(endDateButton) }

        totalExpensesTextView.text = "$${calculateTotal()}"

        setupCategoryTextViews(view)

        view.findViewById<Button>(R.id.btn_filter)?.setOnClickListener {
            filterAndLoadExpenses()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            loadExpenses()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateFromButton(button: Button): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.getDefault())
            LocalDate.parse(button.text.toString(), formatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSelectedCategory(spinner: Spinner): CategoryType? {
        return when (spinner.selectedItem.toString()) {
            "Food" -> CategoryType.FOOD
            "Transport" -> CategoryType.TRANSPORT
            "Entertainment" -> CategoryType.RECREATION
            "Home" -> CategoryType.LIVING
            "Health" -> CategoryType.HEALTH
            "Other" -> CategoryType.OTHER
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterAndLoadExpenses() {
        val startDate = parseDateFromButton(requireView().findViewById(R.id.btn_date_income))
        val endDate = parseDateFromButton(requireView().findViewById(R.id.btn_end_date))
        val category = parseSelectedCategory(requireView().findViewById(R.id.spinner_categories_income))

        viewLifecycleOwner.lifecycleScope.launch {
            val filteredExpenses = expenseRepository.getFilteredExpensesFromFirebase(startDate, endDate, category)
            updateChartWithExpenses(filteredExpenses)
        }
    }

    private fun updateChartWithExpenses(expenses: List<Expense>) {

        categories.forEach { it.total = 0.0f }

        val groupedExpenses = expenses.groupBy { mapCategoryName(it.categoryType) }

        groupedExpenses.forEach { (name, expensesList) ->
            val totalAmount = expensesList.sumOf { it.amount.toDouble() }.toFloat()


            categories.find { it.name == name }?.let {
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
            textView.tag = text // Guardamos el texto original
            categoryTextViews.add(textView)

            // Aseguramos que el color inicial sea el correspondiente
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

            // Configura el clic para la categoría
            setupCategoryClick(textView, text)
        }
    }


    private fun setupCategoryClick(textView: TextView, categoryName: String) {
        textView.setOnClickListener {
            if (selectedCategoryName == categoryName) {
                // Si ya está seleccionada, deselecciona
                selectedCategoryName = null
                clearAllCategorySelections()
                pieChartDrawable.selectedCategory = null
                requireView().findViewById<View>(R.id.graph).invalidate() // Redibuja el gráfico
            } else {
                // Si no está seleccionada, selecciona esta categoría
                selectedCategoryName = categoryName
                highlightSelectedCategory(textView, categoryName)
                pieChartDrawable.selectedCategory = categories.find { it.name == categoryName }
                requireView().findViewById<View>(R.id.graph).invalidate() // Redibuja el gráfico
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



        val percentage = categories.find { it.name == categoryName }?.percentage ?: 0.0f
        selectedTextView.text = "$categoryName  ${"%.1f".format(percentage)}%"

    }



    private fun configureSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinner_categories_income)
        val categoryList = listOf(
            "Food", "Transport", "Entertainment", "Home", "Health", "Other", "All categories"
        )
        val spinnerItems = listOf("Select a Category") + categoryList

        val adapter = object : ArrayAdapter<String>(
            requireContext(), R.layout.spinner_item, spinnerItems
        ) {
            override fun isEnabled(position: Int) = position != 0

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val colorRes = if (position == 0) R.color.edt_text else R.color.txt_color
                textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
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
                        selectedCategoryName = clickedCategory.name
                        pieChartDrawable.selectedCategory = clickedCategory
                        categoryTextViews.find { it.tag == clickedCategory.name }?.let {
                            highlightSelectedCategory(it, clickedCategory.name)
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


        private fun showDatePicker(button: Button) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(), R.style.Nest_DatePicker,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                button.text = selectedDate
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
            },
            year, month, day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar") { _, _ ->
            when (button.id) {
                R.id.btn_date_income -> button.text = "Start date"
                R.id.btn_end_date -> button.text = "End date"
                else -> button.text = "Select date"
            }
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.darker_blue))
        }

        datePickerDialog.setOnShowListener {
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        }

        datePickerDialog.show()
    }




    private fun calculateTotal(): Float {
        return categories.sumOf { it.total.toDouble() }.toFloat()
    }

    private fun mapCategoryName(categoryType: CategoryType): String {
        return when (categoryType) {
            CategoryType.LIVING -> "Home"
            CategoryType.RECREATION -> "Recreation"
            CategoryType.TRANSPORT -> "Transport"
            CategoryType.FOOD -> "Food"
            CategoryType.HEALTH -> "Health"
            CategoryType.OTHER -> "Others"
        }
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
            expenses.filter { mapCategoryName(it.categoryType) == "Food" }.sumOf { it.amount.toDouble() }.toFloat(), 75f,
            expenses.filter { mapCategoryName(it.categoryType) == "Transport" }.sumOf { it.amount.toDouble() }.toFloat(), 1f,
            expenses.filter { mapCategoryName(it.categoryType) == "Health" }.sumOf { it.amount.toDouble() }.toFloat(), 50f,
            expenses.filter { mapCategoryName(it.categoryType) == "Others" }.sumOf { it.amount.toDouble() }.toFloat(), 50f,
            expenses.filter { mapCategoryName(it.categoryType) == "Home" }.sumOf { it.amount.toDouble() }.toFloat(), 41f,
            expenses.filter { mapCategoryName(it.categoryType) == "Recreation" }.sumOf { it.amount.toDouble() }.toFloat(), 20f
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