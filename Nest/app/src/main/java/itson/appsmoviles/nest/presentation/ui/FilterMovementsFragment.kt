package itson.appsmoviles.nest.presentation.ui

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import itson.appsmoviles.nest.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class FilterMovementsFragment : DialogFragment() {
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var spinner: Spinner
    private lateinit var btnFilter: Button
    private lateinit var categories: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_movements, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnStartDate = view.findViewById(R.id.btn_start_date_filter)
        btnEndDate = view.findViewById(R.id.btn_end_date_filter)
        spinner = view.findViewById(R.id.spinner_categories_filter)
        btnFilter = view.findViewById(R.id.btn_filter_movements)
        categories = mutableListOf(
            "Food",
            "Transport",
            "Entertainment",
            "Home",
            "Health",
            "Other"
        )

        setUpSpinner(view)
        btnStartDate.setOnClickListener {
            showDatePicker(btnStartDate)
        }
        btnEndDate.setOnClickListener {
            showDatePicker(btnEndDate)
        }

        btnFilter.setOnClickListener {
            val startDate = btnStartDate.text.toString()
            val endDate = btnEndDate.text.toString()
            val category = spinner.selectedItem.toString()

            if (validateEntries(startDate, endDate, category)) {
                dismiss()
            }
        }


    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent) // Ensures rounded corners
    }

    private fun setUpSpinner(view: View) {

        val spinnerCategories = listOf("Select a Category") + categories

        val adapter =
            object :
                ArrayAdapter<String>(requireContext(), R.layout.spinner_item, spinnerCategories) {
                override fun isEnabled(position: Int): Boolean {

                    return position != 0
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    val textView = view as TextView
                    if (position == 0) {
                        textView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.edt_text
                            )
                        )
                    } else {
                        textView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.txt_color
                            )
                        )
                    }
                    return view
                }
            }

        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker(btnDate: Button) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.Nest_DatePicker,
            { _, year, month, day ->
                val selectedDate = formatDate(day, month, year)

                btnDate.apply {
                    text = selectedDate
                }
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        datePickerDialog.show()

        val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
        val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

        positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val selectedDate = LocalDate.of(year, month + 1, day)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault())
        return selectedDate.format(formatter)
    }

    private fun validateEntries(startDate: String, endDate: String, category: String): Boolean {
        if (startDate.isEmpty() || endDate.isEmpty() || !categories.contains(category)) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (startDate > endDate) {
            Toast.makeText(
                requireContext(),
                "Start date must be before end date",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

}