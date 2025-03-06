package itson.appsmoviles.nest.presentation.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.utilities.PieChartDrawable
import itson.appsmoviles.nest.utilities.Categoria
import itson.appsmoviles.nest.utilities.ExpensesDrawable

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
        return inflater.inflate(R.layout.total_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarSpinner(view)
        val totalExpensesTextView = view.findViewById<TextView>(R.id.totalExpenses)
        totalExpensesTextView.text = "$${calcularTotal()}"

        configurarGrafico(view)

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
        val categories = listOf("Select a category", "Food", "Transport", "Entertainment", "Home", "Health", "Other")

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean = position != 0

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
