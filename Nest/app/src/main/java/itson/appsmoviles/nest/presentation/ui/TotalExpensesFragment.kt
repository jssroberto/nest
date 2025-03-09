package itson.appsmoviles.nest.presentation.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.presentation.utilities.PieChartDrawable
import itson.appsmoviles.nest.presentation.utilities.Categoria
import itson.appsmoviles.nest.presentation.utilities.ExpensesDrawable
import java.util.Calendar

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

        configurarSpinner(view)

        val totalExpensesTextView = view.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView.text = "$${calcularTotal()}"

        configurarGrafico(view)

        calculateFoodExpended(view, 100f, 33.33f)
        calculateTransportExpended(view, 100f, 14.33f)
        calculateHealthExpended(view, 100f, 28.00f)
        calculateOtherExpended(view, 100f, 15.00f)
        calculateHomeExpended(view, 100f, 51.00f)
        calculateRecreationExpended(view, 100f, 89.00f)

        view.findViewById<Button>(R.id.endDate)?.setOnClickListener {
            showEndDatePicker()
        }

        view.findViewById<Button>(R.id.startDate)?.setOnClickListener {
            showStartDatePicker()
        }

    }



    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = "$day/${month + 1}/$year"

                view?.findViewById<Button>(R.id.endDate)?.apply {
                    text = selectedDate
                    setTextColor(Color.parseColor("#0C5A5C")) // Cambia "#FF5733" por el color que desees
                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }


    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = "$day/${month + 1}/$year"

                view?.findViewById<Button>(R.id.startDate)?.apply {
                    text = selectedDate
                    setTextColor(Color.parseColor("#0C5A5C")) // Cambia "#FF5733" por el color que desees
                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }



    private fun calculateFoodExpended(view: View, total: Float, current: Float){
        val foodExpended = view.findViewById<View>(R.id.foodExpended)
        val foodTotal = view.findViewById<View>(R.id.foodBudget)
        val estimatedBudgetFood = view.findViewById<TextView>(R.id.estimatedBudgetFood)
        val actualExpensesFood = view.findViewById<TextView>(R.id.actualExpensesFood)

        estimatedBudgetFood.text =  "$${total}"
        actualExpensesFood.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        foodExpended.background = expendedFood
        foodTotal.background = budgetFood
    }

    private fun calculateHealthExpended(view: View, total: Float, current: Float){
        val healthExpended = view.findViewById<View>(R.id.healthExpended)
        val healthTotal = view.findViewById<View>(R.id.budgetHealth)
        val estimatedBudgetHealth = view.findViewById<TextView>(R.id.estimatedBudgetHealth)
        val actualExpensesHealth = view.findViewById<TextView>(R.id.actualExpensesHealth)

        estimatedBudgetHealth.text =  "$${total}"
        actualExpensesHealth.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        healthExpended.background = expendedFood
        healthTotal.background = budgetFood
    }

    private fun calculateOtherExpended(view: View, total: Float, current: Float){
        val otherExpended = view.findViewById<View>(R.id.othersExpended)
        val otherTotal = view.findViewById<View>(R.id.budgetOthers)
        val estimatedBudgetOthers = view.findViewById<TextView>(R.id.estimatedBudgetOthers)
        val actualExpensesOthers = view.findViewById<TextView>(R.id.actualExpensesOthers)

        estimatedBudgetOthers.text =  "$${total}"
        actualExpensesOthers.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        otherExpended.background = expendedFood
        otherTotal.background = budgetFood
    }


    private fun calculateRecreationExpended(view: View, total: Float, current: Float){
        val recreationExpended = view.findViewById<View>(R.id.recreationExpended)
        val recreationTotal = view.findViewById<View>(R.id.budgetRecreation)
        val estimatedBudgetRecreation = view.findViewById<TextView>(R.id.estimatedBudgetRecreation)
        val actualExpensesRecreation = view.findViewById<TextView>(R.id.actualExpensesRecreation)

        estimatedBudgetRecreation.text =  "$${total}"
        actualExpensesRecreation.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        recreationExpended.background = expendedFood
        recreationTotal.background = budgetFood
    }

    private fun calculateHomeExpended(view: View, total: Float, current: Float){
        val homeExpended = view.findViewById<View>(R.id.homeExpended)
        val homeTotal = view.findViewById<View>(R.id.budgetHome)
        val estimatedBudgetHome = view.findViewById<TextView>(R.id.estimatedBudgetHome)
        val actualExpensesHome = view.findViewById<TextView>(R.id.actualExpensesHome)

        estimatedBudgetHome.text =  "$${total}"
        actualExpensesHome.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        homeExpended.background = expendedFood
        homeTotal.background = budgetFood
    }

    private fun calculateTransportExpended(view: View, total: Float, current: Float){
        val transportExpended = view.findViewById<View>(R.id.transportExpended)
        val transportTotal = view.findViewById<View>(R.id.transportBudget)
        val estimatedBudgetTransport = view.findViewById<TextView>(R.id.estimatedBudgetTransport)
        val actualExpensesTransport = view.findViewById<TextView>(R.id.actualExpensesTransport)

        estimatedBudgetTransport.text =  "$${total}"
        actualExpensesTransport.text =  "$${current}"

        val progressColor = ContextCompat.getColor(requireContext(), R.color.blue)
        val progressTotalColor = ContextCompat.getColor(requireContext(), R.color.darker_blue)
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.off_white)

        val budgetFood = ExpensesDrawable(total, total, progressTotalColor, backgroundColor)
        val expendedFood = ExpensesDrawable(total, current, progressColor, backgroundColor)

        transportExpended.background = expendedFood
        transportTotal.background = budgetFood
    }

    private fun calcularTotal(): Float {
        return categorias.sumOf { it.total.toDouble() }.toFloat()
    }


    private fun configurarGrafico(view: View) {
        val graph = view.findViewById<View>(R.id.graph)
        val pieChartDrawable = PieChartDrawable(requireContext(), categorias)
        graph.background = pieChartDrawable



    }

    private fun configurarSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerCategories)
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
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.off_white))
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
