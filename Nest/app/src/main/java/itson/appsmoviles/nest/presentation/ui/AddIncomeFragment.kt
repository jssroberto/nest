package itson.appsmoviles.nest.presentation.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import itson.appsmoviles.nest.R
import java.util.Calendar


class AddIncomeFragment : Fragment() {

    private lateinit var btnDate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarSpinner(view)

        btnDate = view.findViewById<Button>(R.id.btn_date_income)

        btnDate.setOnClickListener {
            showStartDatePicker()
        }

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

    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = "$day/${month + 1}/$year"

                view?.findViewById<Button>(R.id.btn_date_income)?.apply {
                    text = selectedDate
                    setTextColor(Color.parseColor("#FFFFFF")) // Cambia "#FF5733" por el color que desees
                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }
}
