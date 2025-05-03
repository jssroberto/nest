package itson.appsmoviles.nest.ui.add.expense

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.util.addDollarSign
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import itson.appsmoviles.nest.ui.util.toTitleCase

@RequiresApi(Build.VERSION_CODES.O)
class AddExpenseFragment : Fragment() {
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnAddExpense: Button
    private lateinit var spinner: Spinner
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton
    private var selectedTimestamp: Long? = null

    private lateinit var viewModel: AddExpenseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AddExpenseViewModel::class.java]

        edtAmount = view.findViewById(R.id.edt_amount_expense)
        edtDescription = view.findViewById(R.id.edt_description_expense)
        btnDate = view.findViewById(R.id.btn_date_expense)
        spinner = view.findViewById(R.id.spinner_categories_expense)
        radioCash = view.findViewById(R.id.radio_cash)
        radioCard = view.findViewById(R.id.radio_card)
        btnAddExpense = view.findViewById(R.id.btn_add_expense)

        setUpSpinner()
        addDollarSign(edtAmount)

        setUpClickListeners()

        setRadioColors()

        viewModel.fetchExpenses()

        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            Log.d("AddExpenseFragment", "Expenses: $expenses")

        }

    }

    private fun setUpClickListeners() {
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

        btnAddExpense.setOnClickListener {
            addExpense()
        }
    }

    private fun addExpense() {
        if (!validateFields()) {
            showToast(requireContext(), "Please fill in all fields")
            return
        }

        val expense = Expense(
            id = "",
            description = edtDescription.text.toString().trim(),
            amount = getAmount(),
            date = selectedTimestamp ?: 0L,
            category = CategoryType.fromDisplayName(spinner.selectedItem.toString()),
            paymentMethod = if (radioCash.isChecked) PaymentMethod.CASH else PaymentMethod.CARD
        )

        viewModel.addExpense(
            expense = expense,
            onSuccess = {
                clearFields()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onFailure = { e ->
                showToast(requireContext(), "Error adding expense: ${e.message}")
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


    private fun setRadioColors() {
        radioCash.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)
        radioCard.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)
    }


    private fun clearFields() {
        edtAmount.text.clear()
        spinner.setSelection(0)
        edtDescription.text.clear()
        btnDate.text = getString(R.string.select_date)
        selectedTimestamp = null
        radioCash.isChecked = false
        radioCard.isChecked = false
    }

    private fun getAmount(): Double {
        return edtAmount.text.toString().replace("$", "").replace(",", ".").toDouble()
    }

    private fun validateFields(): Boolean {
        return edtAmount.text.isNotEmpty() && spinner.selectedItemPosition != 0 &&
                edtDescription.text.isNotEmpty() && selectedTimestamp != null &&
                (radioCash.isChecked || radioCard.isChecked) && selectedTimestamp != null
    }
}