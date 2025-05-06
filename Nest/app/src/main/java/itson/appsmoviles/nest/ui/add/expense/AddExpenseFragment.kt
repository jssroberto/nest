package itson.appsmoviles.nest.ui.add.expense

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.addDollarSign
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast

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

    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()

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

        setUpSpinner(requireContext(), spinner)
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
        if (!areFieldsValid()) {
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
                sharedMovementsViewModel.notifyMovementsUpdated()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onFailure = { e ->
                showToast(requireContext(), "Error adding expense: ${e.message}")
            }
        )
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

    private fun areFieldsValid(): Boolean {
        if (edtAmount.text.isNullOrEmpty()) {
            Toast.makeText(context, "Please enter an amount.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spinner.selectedItemPosition == 0) {
            Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (edtDescription.text.isNullOrEmpty()) {
            Toast.makeText(context, "Please enter a description.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedTimestamp == null) {
            Toast.makeText(context, "Please select a date.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!radioCash.isChecked && !radioCard.isChecked) {
            Toast.makeText(context, "Please select a payment method (Cash or Card).", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}