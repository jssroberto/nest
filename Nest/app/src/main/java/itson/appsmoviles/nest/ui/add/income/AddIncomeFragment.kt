package itson.appsmoviles.nest.ui.add.income

import android.R.attr.category
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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.ui.util.addDollarSign
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class AddIncomeFragment : Fragment() {

    private lateinit var edtAmount: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var btnDate: Button
    private var selectedTimestamp: Long? = null
    private lateinit var viewModel: AddIncomeViewModel
    private lateinit var addIncomeButton: Button

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

        edtAmount = view.findViewById(R.id.edt_amount_income)
        btnDate = view.findViewById<Button>(R.id.btn_date_income)
        descriptionEditText = view.findViewById(R.id.edt_description_income)
        addIncomeButton = view.findViewById<Button>(R.id.btn_add_income)
        viewModel = ViewModelProvider(this)[AddIncomeViewModel::class.java]

        addDollarSign(edtAmount)

        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        addIncomeButton.setOnClickListener {
            addIncome()
        }

        btnDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                onDateSelected = { timestampMillis ->
                    selectedTimestamp = timestampMillis
                    btnDate.apply {
                        text = formatDateLongForm(timestampMillis)
                    }
                }
            )
        }
    }

    private fun addIncome() {
        val amountStr = edtAmount.text.toString().replace("$", "").trim()
        val description = descriptionEditText.text.toString().trim()

        if (amountStr.isBlank() || selectedTimestamp == null || description.isBlank()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val income = Income(
            id = "",
            description = description,
            amount = amount,
            date = selectedTimestamp!!
        )

        viewModel.addIncome(
            income = income,
            onSuccess = {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onFailure = { exception ->
                showToast(requireContext(), "Error adding income: ${exception.message}")
            }
        )
    }
}