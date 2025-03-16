package itson.appsmoviles.nest.presentation.ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


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

        btnDate = view.findViewById<Button>(R.id.btn_date_income)

        setUpSpinner(view)

        btnDate.setOnClickListener {
            showStartDatePicker()
        }

    }


    private fun setUpSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerCategories)
        val categories = listOf("Select a category", "Food", "Transport", "Entertainment", "Home", "Health", "Other")

        val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                // Disable the hint item
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                if (position == 0) {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.edt_text)) // Hint color
                } else {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color)) // Normal color
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.Nest_DatePicker,
            { _, year, month, day ->
                val selectedDate = formatDate(day, month, year)

                view?.findViewById<Button>(R.id.btn_date_income)?.apply {
                    text = selectedDate
                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()

        val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
        val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

        // Set the color of the "OK" and "Cancel" buttons
        positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val selectedDate = LocalDate.of(year, month + 1, day)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        return selectedDate.format(formatter)
    }
}
