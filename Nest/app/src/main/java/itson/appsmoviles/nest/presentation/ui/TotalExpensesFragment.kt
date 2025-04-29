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

    private val categorias = arrayListOf(
        Categoria("Health", 0.0f, R.color.lightest_blue, 0.0f),
        Categoria("Home", 0.0f, R.color.lighter_blue, 0.0f),
        Categoria("Food", 0.0f, R.color.light_blue, 0.0f),
        Categoria("Recreation", 0.0f, R.color.dark_blue, 0.0f),
        Categoria("Transport", 0.0f, R.color.darker_blue, 0.0f),
        Categoria("Others", 0.0f, R.color.blue, 0.0f)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_total_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startDate: Button = view.findViewById(R.id.btn_date_income)
        val endDate: Button = view.findViewById(R.id.btn_end_date)

        configurarSpinner(view)



        val totalExpensesTextView = view.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView.text = "$${calcularTotal()}"

        loadExpenses()
        configurarGrafico(view)

        startDate.setOnClickListener {
            showDatePicker(startDate)
        }

        endDate.setOnClickListener {
            showDatePicker(endDate)
        }


    }


    private fun calculateExpendedGraphic(
        view: View,
        totalFood: Float, currentFood: Float,
        totalTransport: Float, currentTransport: Float,
        totalHealth: Float, currentHealth: Float,
        totalOthers: Float, currentOthers: Float,
        totalHome: Float, currentHome: Float,
        totalRecreation: Float, currentRecreation: Float
    ) {
        calculateExpended(view.findViewById(R.id.foodBudget), totalFood, currentFood)
        calculateExpended(view.findViewById(R.id.transportBudget), totalTransport, currentTransport)
        calculateExpended(view.findViewById(R.id.budgetHealth), totalHealth, currentHealth)
        calculateExpended(view.findViewById(R.id.budgetOthers), totalOthers, currentOthers)
        calculateExpended(view.findViewById(R.id.budgetHome), totalHome, currentHome)
        calculateExpended(view.findViewById(R.id.budgetRecreation), totalRecreation, currentRecreation)
    }

    private fun calculateExpended(expendedView: View, total: Float, current: Float) {
        val progressColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.txt_income)
        val expendedDrawable = ExpensesDrawable(total, current, progressColor, backgroundColor)
        expendedView.background = expendedDrawable
    }

    private fun showDatePicker(btnDate: Button) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.Nest_DatePicker,
            { _, year, month, day ->
                val selectedDate = "$day/${month + 1}/$year"
                btnDate.text = selectedDate
                btnDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
            },
            currentYear, currentMonth, currentDay
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

    private fun calcularTotal(): Float {
        return categorias.sumOf { it.total.toDouble() }.toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarGrafico(view: View) {
        val graph = view.findViewById<View>(R.id.graph)
        val pieChartDrawable = PieChartDrawable(requireContext(), categorias)
        graph.background = pieChartDrawable

        graph.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                pieChartDrawable.onTouch(event.x, event.y)
                graph.invalidate()
            }
            true
        }
    }

    private fun configurarSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinner_categories_income)
        val categories = listOf(
            "Food",
            "Transport",
            "Entertainment",
            "Home",
            "Health",
            "Other",
            "All categories"
        )
        val spinnerCategories = listOf("Select a Category") + categories

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item,
            spinnerCategories
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

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


    private fun loadExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            val expenses = expenseRepository.getMovementsFromFirebase()

            // Agrupar gastos por categoría
            val grouped = expenses.groupBy { mapCategoryName(it.category) }

            // Crear nueva lista de categorías con sus totales calculados
            categorias.clear()
            grouped.forEach { (nombre, gastos) ->
                val total = gastos.sumOf { it.amount.toDouble() }.toFloat()

                // Asignar color según nombre
                val color = when (nombre) {
                    "Health" -> R.color.category_health
                    "Home" -> R.color.category_living
                    "Food" -> R.color.category_food
                    "Recreation" -> R.color.category_recreation
                    "Transport" -> R.color.category_transport
                    "Others" -> R.color.category_other
                    else -> R.color.gray // por si acaso
                }

                categorias.add(Categoria(nombre, 0f, color, total))
            }

            // Actualizar UI con nueva lista de categorías
            actualizarGrafico()
            actualizarTotal()

            // Si usás esto para los gráficos de progreso
            calculateExpendedGraphic(
                requireView(),
                categorias.find { it.nombre == "Food" }?.total ?: 0f, 75f,
                categorias.find { it.nombre == "Transport" }?.total ?: 0f, 1f,
                categorias.find { it.nombre == "Health" }?.total ?: 0f, 50f,
                categorias.find { it.nombre == "Others" }?.total ?: 0f, 50f,
                categorias.find { it.nombre == "Home" }?.total ?: 0f, 41f,
                categorias.find { it.nombre == "Recreation" }?.total ?: 0f, 20f
            )
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun actualizarGrafico() {
        Log.d("Grafico", "Actualizando gráfico con ${categorias.size} categorías")
        val graph = view?.findViewById<View>(R.id.graph)
        val pieChartDrawable = PieChartDrawable(requireContext(), categorias)
        graph?.background = pieChartDrawable

        graph?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                pieChartDrawable.onTouch(event.x, event.y)
                graph.invalidate()
            }
            true
        }
    }


    private fun actualizarTotal() {
        val totalExpensesTextView = view?.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView?.text = "$${calcularTotal()}"
    }



}
