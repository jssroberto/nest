package itson.appsmoviles.nest.ui.add.expense

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.enums.PaymentMethod
import itson.appsmoviles.nest.ui.util.toTitleCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment : Fragment() {
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnAddExpense: Button
    private lateinit var spinner: Spinner
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton
    private var selectedDate: String? = null

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        edtAmount = view.findViewById(R.id.edt_amount_expense)
        edtDescription = view.findViewById(R.id.edt_description_expense)
        btnDate = view.findViewById(R.id.btn_date_expense)
        spinner = view.findViewById(R.id.spinner_categories_expense)
        radioCash = view.findViewById(R.id.radio_cash)
        radioCard = view.findViewById(R.id.radio_card)
        btnAddExpense = view.findViewById(R.id.btn_add_expense)

        setUpSpinner()
        addDollarSign(edtAmount)

        btnDate.setOnClickListener {
            showDatePicker(btnDate)
        }

        btnAddExpense.setOnClickListener {
            addExpense()
        }

        setRadioColors()

        viewModel.fetchExpenses()

        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            Log.d("AddExpenseFragment", "Gastos obtenidos: $expenses")

        }
    }

    private fun addExpense() {
        if (!validarCampos()) {
            Toast.makeText(
                requireContext(),
                "Por favor, completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val monto = obtenerMonto()
        val descripcion = edtDescription.text.toString().trim()
        val categoria = CategoryType.fromDisplayName(spinner.selectedItem.toString())
        val metodoPago = if (radioCash.isChecked) PaymentMethod.CASH else PaymentMethod.CARD

        viewModel.addExpense(
            monto,
            descripcion,
            categoria,
            metodoPago.name,
            selectedDate!!,
            onSuccess = {
                limpiarCampos()
                requireActivity().onBackPressedDispatcher.onBackPressed()

            },
            onFailure = { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }


    private fun setUpSpinner() {
        val categories =
            listOf("Select a Category") + CategoryType.entries.map { it.name.toTitleCase() }

        val adapter =
            object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, categories) {
                override fun isEnabled(position: Int): Boolean {
                    return position != 0
                }
            }
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
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
                selectedDate = "$day/${month + 1}/$year"

                btnDate.apply {
                    text = selectedDate
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
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
        return LocalDate.of(year, month + 1, day)
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
    }

    private fun addDollarSign(edtAmount: EditText) {
        edtAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                edtAmount.removeTextChangedListener(this)
                val input = editable.toString()
                edtAmount.setText(if (!input.startsWith("$")) "$$input" else input)
                edtAmount.setSelection(edtAmount.text.length)
                edtAmount.addTextChangedListener(this)
            }
        })
    }

    private fun setRadioColors() {
        radioCash.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)
        radioCard.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)
    }


    private fun limpiarCampos() {
        edtAmount.text.clear()
        spinner.setSelection(0)
        edtDescription.text.clear()
        btnDate.text = getString(R.string.select_date)
        selectedDate = null
        radioCash.isChecked = false
        radioCard.isChecked = false
    }

    private fun obtenerMonto(): Double {
        return edtAmount.text.toString().replace("$", "").replace(",", ".").toDouble()
    }

    private fun validarCampos(): Boolean {
        return edtAmount.text.isNotEmpty() && spinner.selectedItemPosition != 0 &&
                edtDescription.text.isNotEmpty() && selectedDate != null &&
                (radioCash.isChecked || radioCard.isChecked)
    }
}