package itson.appsmoviles.nest.presentation.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.presentation.utilities.PieChartDrawable
import itson.appsmoviles.nest.presentation.utilities.Categoria
import itson.appsmoviles.nest.presentation.utilities.ExpensesDrawable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TotalExpensesFragment : Fragment() {

    private val categorias = arrayListOf(
        Categoria("Health", 25.0f, R.color.lightest_blue, 156.0f),
        Categoria("Home", 15.0f, R.color.lighter_blue, 78.0f),
        Categoria("Food", 10.0f, R.color.light_blue, 62.4f),
        Categoria("Recreation", 20.0f, R.color.dark_blue, 104.0f),
        Categoria("Transport", 15.0f, R.color.darker_blue, 104.0f),
        Categoria("Others", 15.0f, R.color.blue, 104.0f)
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

        calculateExpendedGraphic(view, 150f,75f,200f,100f,400f,50f, 100f,50f,40f, 50f, 50f, 20f)

        configurarSpinner(view)

        val totalExpensesTextView = view.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView.text = "$${calcularTotal()}"

        configurarGrafico(view)



        view.findViewById<Button>(R.id.btn_end_date)?.setOnClickListener {
            showDatePicker(endDate)
        }

        view.findViewById<Button>(R.id.btn_date_income)?.setOnClickListener {
            showDatePicker(startDate)
        }

    }


    private fun calculateExpendedGraphic(view: View,
                                         totalFood: Float, currentFood: Float,
                                         totalTransport: Float, currentTransport: Float,
                                         totalHealth: Float, currentHealth: Float,
                                         totalOthers: Float, currentOthers: Float,
                                         totalHome: Float, currentHome: Float,
                                         totalRecreation: Float, currentRecreation: Float,){
        val foodTotal = view.findViewById<View>(R.id.foodBudget)
        val estimatedBudgetFood = view.findViewById<TextView>(R.id.estimatedBudgetFood)
        val actualExpensesFood = view.findViewById<TextView>(R.id.actualExpensesFood)
        val healthTotal = view.findViewById<View>(R.id.budgetHealth)
        val estimatedBudgetHealth = view.findViewById<TextView>(R.id.estimatedBudgetHealth)
        val actualExpensesHealth = view.findViewById<TextView>(R.id.actualExpensesHealth)
        val otherTotal = view.findViewById<View>(R.id.budgetOthers)
        val estimatedBudgetOthers = view.findViewById<TextView>(R.id.estimatedBudgetOthers)
        val actualExpensesOthers = view.findViewById<TextView>(R.id.actualExpensesOthers)
        val recreationTotal = view.findViewById<View>(R.id.budgetRecreation)
        val estimatedBudgetRecreation = view.findViewById<TextView>(R.id.estimatedBudgetRecreation)
        val actualExpensesRecreation = view.findViewById<TextView>(R.id.actualExpensesRecreation)
        val homeTotal = view.findViewById<View>(R.id.budgetHome)
        val estimatedBudgetHome = view.findViewById<TextView>(R.id.estimatedBudgetHome)
        val actualExpensesHome = view.findViewById<TextView>(R.id.actualExpensesHome)
        val transportTotal = view.findViewById<View>(R.id.transportBudget)
        val estimatedBudgetTransport = view.findViewById<TextView>(R.id.estimatedBudgetTransport)
        val actualExpensesTransport = view.findViewById<TextView>(R.id.actualExpensesTransport)


        calculateExpended(
            view.findViewById(R.id.foodBudget),
            view.findViewById(R.id.estimatedBudgetFood),
            view.findViewById(R.id.actualExpensesFood),
            totalFood, currentFood
        )

        calculateExpended(
            view.findViewById(R.id.transportBudget),
            view.findViewById(R.id.estimatedBudgetTransport),
            view.findViewById(R.id.actualExpensesTransport),
            totalTransport, currentTransport
        )

        calculateExpended(
            view.findViewById(R.id.budgetHealth),
            view.findViewById(R.id.estimatedBudgetHealth),
            view.findViewById(R.id.actualExpensesHealth),
            totalHealth, currentHealth
        )

        calculateExpended(
            view.findViewById(R.id.budgetOthers),
            view.findViewById(R.id.estimatedBudgetOthers),
            view.findViewById(R.id.actualExpensesOthers),
            totalOthers, currentOthers
        )

        calculateExpended(
            view.findViewById(R.id.budgetHome),
            view.findViewById(R.id.estimatedBudgetHome),
            view.findViewById(R.id.actualExpensesHome),
            totalHome, currentHome
        )

        calculateExpended(
            view.findViewById(R.id.budgetRecreation),
            view.findViewById(R.id.estimatedBudgetRecreation),
            view.findViewById(R.id.actualExpensesRecreation),
            totalRecreation, currentRecreation
        )

    }

    private fun calculateExpended(
        expendedView: View,
        estimatedBudgetTextView: TextView,
        actualExpensesTextView: TextView,
        total: Float,
        current: Float
    ) {
        estimatedBudgetTextView.text = "$${total}"
        actualExpensesTextView.text = "$${current}"

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

                btnDate.apply {
                    text = selectedDate
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))

                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()

        val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
        val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

        positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
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
        val categories = listOf("Select a category", "Food", "Transport", "Entertainment", "Home", "Health", "Other", "All categories")

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, categories) {


            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_regular)
                textView.textSize = 16f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.edt_text))
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_regular)
                textView.textSize = 16f
                textView.setPadding(20, 20, 20, 20)
                return view
            }
        }

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
