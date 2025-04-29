package itson.appsmoviles.nest.presentation.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.domain.model.repository.ExpenseRepository
import itson.appsmoviles.nest.presentation.utilities.PieChartDrawable
import itson.appsmoviles.nest.presentation.utilities.Categoria
import itson.appsmoviles.nest.presentation.utilities.ExpensesDrawable
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TotalExpensesFragment : Fragment() {

    private val expenseRepository = ExpenseRepository()
    private val categories = arrayListOf(
        Categoria("Health", 0.0f, R.color.lightest_blue, 0.0f),
        Categoria("Home", 0.0f, R.color.lighter_blue, 0.0f),
        Categoria("Food", 0.0f, R.color.light_blue, 0.0f),
        Categoria("Recreation", 0.0f, R.color.dark_blue, 0.0f),
        Categoria("Transport", 0.0f, R.color.darker_blue, 0.0f),
        Categoria("Others", 0.0f, R.color.blue, 0.0f)
    )

    private var selectedCategoryName: String? = null
    private lateinit var pieChartDrawable: PieChartDrawable
    private val categoryTextViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_total_expenses, container, false)
    }

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

        viewLifecycleOwner.lifecycleScope.launch {
            loadExpenses()
        }
    }

    private fun updateCategoryTextViews() {
        categoryTextViews.forEach { textView ->
            val categoryName = textView.tag?.toString()
            if (selectedCategoryName == null) {
                // Si no hay selección, todos en negro
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            } else {
                // Si hay algo seleccionado
                if (categoryName == selectedCategoryName) {
                    // El seleccionado en negro
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    // Los demás transparentes
                    textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                }
            }
        }
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
                pieChartDrawable.selectedCategoria = null
                requireView().findViewById<View>(R.id.graph).invalidate() // Redibuja el gráfico
            } else {
                // Si no está seleccionada, selecciona esta categoría
                selectedCategoryName = categoryName
                highlightSelectedCategory(textView, categoryName)
                pieChartDrawable.selectedCategoria = categories.find { it.nombre == categoryName }
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
        clearAllCategorySelections() // Limpia las selecciones previas

        // Resalta la categoría seleccionada
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        // Asigna el icono correspondiente
        val drawableRes = when (categoryName) {
            "Food" -> R.drawable.icon_category_food
            "Transport" -> R.drawable.icon_category_transport
            "Health" -> R.drawable.icon_category_health
            "Home" -> R.drawable.home_filled
            "Recreation" -> R.drawable.icon_category_recreation
            "Others" -> R.drawable.icon_category_other
            else -> 0
        }

        val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        selectedTextView.setCompoundDrawables(null, drawable, null, null)

        // Muestra el porcentaje de la categoría seleccionada
        val percentage = categories.find { it.nombre == categoryName }?.porcentaje ?: 0.0f
        selectedTextView.text = "$categoryName\n${"%.1f".format(percentage)}%"
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
                // Cuando tocas el gráfico: cancelar selección
                if (selectedCategoryName != null) {
                    selectedCategoryName = null
                    clearAllCategorySelections()
                    pieChartDrawable.selectedCategoria = null
                    graphView.invalidate()
                }
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

        datePickerDialog.setOnShowListener {
            val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        }

        datePickerDialog.show()
    }

    private fun calculateTotal(): Float {
        return categories.sumOf { it.total.toDouble() }.toFloat()
    }

    private fun mapCategoryName(category: Category): String {
        return when (category) {
            Category.LIVING -> "Home"
            Category.RECREATION -> "Recreation"
            Category.TRANSPORT -> "Transport"
            Category.FOOD -> "Food"
            Category.HEALTH -> "Health"
            Category.OTHER -> "Others"
        }
    }

    private suspend fun loadExpenses() {
        val expenses = expenseRepository.getMovementsFromFirebase()

        val groupedExpenses = expenses.groupBy { mapCategoryName(it.category) }

        categories.clear()
        groupedExpenses.forEach { (name, expensesList) ->
            val totalAmount = expensesList.sumOf { it.amount.toDouble() }.toFloat()
            val colorRes = when (name) {
                "Health" -> R.color.category_health
                "Home" -> R.color.category_living
                "Food" -> R.color.category_food
                "Recreation" -> R.color.category_recreation
                "Transport" -> R.color.category_transport
                "Others" -> R.color.category_other
                else -> R.color.gray
            }
            categories.add(Categoria(name, 0f, colorRes, totalAmount))
        }

        updatePieChart()
        updateTotal()
        calculateProgressBars(
            requireView(),
            categories.find { it.nombre == "Food" }?.total ?: 0f, 75f,
            categories.find { it.nombre == "Transport" }?.total ?: 0f, 1f,
            categories.find { it.nombre == "Health" }?.total ?: 0f, 50f,
            categories.find { it.nombre == "Others" }?.total ?: 0f, 50f,
            categories.find { it.nombre == "Home" }?.total ?: 0f, 41f,
            categories.find { it.nombre == "Recreation" }?.total ?: 0f, 20f
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updatePieChart() {
        val graphView = view?.findViewById<View>(R.id.graph)
        pieChartDrawable = PieChartDrawable(requireContext(), categories)
        graphView?.background = pieChartDrawable

        graphView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                pieChartDrawable.onTouch(event.x, event.y)
                graphView.invalidate()
            }
            true
        }
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
