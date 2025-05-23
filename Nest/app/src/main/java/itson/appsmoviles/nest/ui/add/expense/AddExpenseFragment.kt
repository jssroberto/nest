package itson.appsmoviles.nest.ui.add.expense

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.add.AddFragment
import itson.appsmoviles.nest.ui.budget.BudgetViewModel
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker

@RequiresApi(Build.VERSION_CODES.O)
class AddExpenseFragment : Fragment() {

    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()
    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var budgetViewModel: BudgetViewModel

    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnAddExpense: Button
    private lateinit var spinner: Spinner
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton

    private var selectedTimestamp: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }
    private fun bindViews(view: View) {

        edtAmount = view.findViewById(R.id.edt_amount_expense)
        edtDescription = view.findViewById(R.id.edt_description_expense)
        btnDate = view.findViewById(R.id.btn_date_expense)
        spinner = view.findViewById(R.id.spinner_categories_expense)
        radioCash = view.findViewById(R.id.radio_cash)
        radioCard = view.findViewById(R.id.radio_card)
        btnAddExpense = view.findViewById(R.id.btn_add_expense)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AddExpenseViewModel::class.java]
        budgetViewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]
        bindViews(view)
        setUpSpinner(requireContext(), spinner)
        setUpCurrencyField()
        setUpListeners()
        viewModel.fetchExpenses()
    }


    private fun setUpListeners() {
        btnDate.setOnClickListener {
            showDatePicker(requireContext()) {
                selectedTimestamp = it
                btnDate.text = formatDateLongForm(it)
            }
        }

        btnAddExpense.setOnClickListener {
            if (!areFieldsValid()) return@setOnClickListener


            val newExpenseAmount = getAmount()
            if (newExpenseAmount <= 0) {
                show("Amount must be greater than zero.")
                return@setOnClickListener
            }

            val selectedCategoryString = spinner.selectedItem.toString()
            val category = CategoryType.fromDisplayName(selectedCategoryString)

            val expense = Expense(
                id = "",
                description = edtDescription.text.toString().trim(),
                amount = newExpenseAmount,
                date = selectedTimestamp
                    ?: System.currentTimeMillis(),
                category = category,
                paymentMethod = if (radioCash.isChecked) PaymentMethod.CASH else PaymentMethod.CARD
            )


            val alarmThresholdForCategory =
                budgetViewModel.alarmThresholdMap[category]?.toDouble() ?: 0.0
            val isAlarmEnabledForCategory = budgetViewModel.alarmEnabledMap[category] == true
            val currentSpentInCategory = viewModel.expenses.value
                ?.filter { it.category == category }
                ?.sumOf { it.amount } ?: 0.0



            viewModel.addExpense(
                expense = expense,
                context = requireContext().applicationContext,
                currentSpentInCategory = currentSpentInCategory,
                alarmThreshold = alarmThresholdForCategory,
                isAlarmEnabled = isAlarmEnabledForCategory,
                onSuccess = {
                    sharedMovementsViewModel.notifyMovementDataChanged()
                    requireActivity().supportFragmentManager.popBackStack(
                        AddFragment.TAG,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                },
                onFailure = { exception ->
                    show("Error adding expense: ${exception.message}")
                }
            )
        }


        radioCash.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)
        radioCard.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)
    }

    private fun getAmount(): Double {
        return edtAmount.text.toString().replace("$", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun areFieldsValid(): Boolean {
        return when {
            edtAmount.text.isNullOrEmpty() -> show("Please enter an amount.").let { false }
            spinner.selectedItemPosition == 0 -> show("Please select a category.").let { false }
            edtDescription.text.isNullOrEmpty() -> show("Please enter a description.").let { false }
            selectedTimestamp == null -> show("Please select a date.").let { false }
            !radioCash.isChecked && !radioCard.isChecked -> show("Please select a payment method.").let { false }
            else -> true
        }
    }

    private fun show(msg: String) {
        if (isAdded && activity != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        } else {

            Log.w("AddExpenseFragment", "Cannot show toast, fragment not added or activity is null: $msg")
        }
    }
    private fun setUpCurrencyField() {
        edtAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.")

        val maxBefore = 7
        val maxAfter = 2

        edtAmount.filters = arrayOf(InputFilter.LengthFilter(maxBefore + maxAfter + 2))

        edtAmount.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                val clean = s.toString().replace("[^\\d.]".toRegex(), "")
                val parts = clean.split('.')

                val integerPart = parts.getOrNull(0)?.take(maxBefore) ?: ""
                val decimalPart = parts.getOrNull(1)?.take(maxAfter) ?: ""

                val formatted = when {
                    decimalPart.isNotEmpty() -> "$$integerPart.$decimalPart"
                    clean.contains(".") -> "$$integerPart."
                    else -> "$$integerPart"
                }

                edtAmount.setText(formatted)
                edtAmount.setSelection(formatted.length)
                isEditing = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


}
