package itson.appsmoviles.nest.ui.add.income

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddIncomeFragment : Fragment() {

    private lateinit var edtAmount: EditText
    private lateinit var btnDate: Button
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_income, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtAmount = view.findViewById(R.id.edt_amount_income)
        btnDate = view.findViewById<Button>(R.id.btn_date_income)
        spinner = view.findViewById<Spinner>(R.id.spinner_categories_income)


        setUpSpinner(view)

        addDollarSign(edtAmount)

        btnDate.setOnClickListener {
            showDatePicker(btnDate)
        }

        val btnAddIncome = view.findViewById<Button>(R.id.btn_add_income)
        val incomeViewModel = ViewModelProvider(this)[IncomeViewModel::class.java]

        btnAddIncome.setOnClickListener {
            val amountStr = edtAmount.text.toString().replace("$", "").trim()
            val category = spinner.selectedItem.toString()
            val date = btnDate.text.toString()

            if (amountStr.isBlank() || category == "Select a category" || date == "Select Date") {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            incomeViewModel.addIncome(amount, category, date)
        }

        incomeViewModel.isIncomeAdded.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Income added successfully!", Toast.LENGTH_SHORT).show()
                // Optional: clear fields or navigate
            } else {
                Toast.makeText(requireContext(), "Failed to add income", Toast.LENGTH_SHORT).show()
            }
        }


    }


    private fun setUpSpinner(view: View) {
        val categories = listOf(
            "Select a category",
            "Food",
            "Transport",
            "Entertainment",
            "Home",
            "Health",
            "Other"
        )

        val adapter =
            object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, categories) {
                override fun isEnabled(position: Int): Boolean {
                    // Disable the hint item
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
                        ) // Hint color
                    } else {
                        textView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.txt_color
                            )
                        ) // Normal color
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
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        return selectedDate.format(formatter)
    }

    private fun addDollarSign(edtAmount: EditText) {
        edtAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                edtAmount.removeTextChangedListener(this)

                val input = editable.toString()

                val formattedInput = if (!input.startsWith("$")) {
                    "$$input"
                } else {
                    input
                }

                edtAmount.setText(formattedInput)
                edtAmount.setSelection(formattedInput.length)
                edtAmount.addTextChangedListener(this)
            }
        })
    }
}