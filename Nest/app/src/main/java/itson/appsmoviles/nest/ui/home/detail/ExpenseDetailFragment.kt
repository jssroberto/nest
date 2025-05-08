package itson.appsmoviles.nest.ui.home.detail

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class ExpenseDetailFragment : DialogFragment() {

    private val sharedViewModel: SharedMovementsViewModel by activityViewModels()

    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnDate: Button
    private lateinit var categoryImageView: ImageView
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton
    private lateinit var btnSave: Button
    private lateinit var btnDelete: ImageButton

    private var selectedTimestamp: Long? = null
    private lateinit var expense: Expense

    companion object {
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_AMOUNT = "amount"
        private const val ARG_DATE = "date"
        private const val ARG_CATEGORY = "category"
        private const val ARG_ID = "id"
        private const val ARG_PAYMENT_METHOD = "paymentMethod"
        const val REQUEST_KEY_UPDATE_EXPENSE = "update_expense_result"
        const val REQUEST_KEY_DELETE_EXPENSE = "delete_expense_result"
        const val BUNDLE_KEY_UPDATED = "updated"
        const val BUNDLE_KEY_DELETED = "deleted"

        fun newInstance(expense: Expense): ExpenseDetailFragment {
            return ExpenseDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, expense.id)
                    putString(ARG_DESCRIPTION, expense.description)
                    putDouble(ARG_AMOUNT, expense.amount)
                    putLong(ARG_DATE, expense.date)
                    putString(ARG_CATEGORY, expense.category.name)
                    putString(
                        ARG_PAYMENT_METHOD,
                        expense.paymentMethod.name
                    ) // Ensure this is passed
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_expense_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadExpenseFromArguments()
        populateUiWithExpenseData()
        setupListeners()
        setRadioColors()
    }

    private fun initializeViews(view: View) {
        etDescription = view.findViewById(R.id.et_description_expense_detail)
        etAmount = view.findViewById(R.id.et_amount_expense_detail)
        btnDate = view.findViewById(R.id.btn_date_expense_detail)
        categoryImageView = view.findViewById(R.id.iv_icon_expense_detail)
        btnSave = view.findViewById(R.id.btn_save_expense_detail)
        radioCash = view.findViewById(R.id.radio_cash_expense_detail)
        radioCard = view.findViewById(R.id.radio_card_expense_detail)
        btnDelete = view.findViewById(R.id.btn_delete_expense_detail)
    }

    private fun loadExpenseFromArguments() {
        val args = arguments ?: return

        val categoryName = args.getString(ARG_CATEGORY) ?: CategoryType.OTHER.name
        val categoryType =
            CategoryType.entries.find { it.name == categoryName } ?: CategoryType.OTHER

        val paymentMethodName = args.getString(ARG_PAYMENT_METHOD)
        val paymentMethodType = paymentMethodName?.let { name ->
            PaymentMethod.entries.find { it.name.equals(name, ignoreCase = true) }
        } ?: PaymentMethod.UNKNOWN


        expense = Expense(
            id = args.getString(ARG_ID) ?: "",
            description = args.getString(ARG_DESCRIPTION) ?: "",
            amount = args.getDouble(ARG_AMOUNT, 0.0),
            date = args.getLong(ARG_DATE, 0L),
            category = categoryType,
            paymentMethod = paymentMethodType
        )
        selectedTimestamp = expense.date
    }

    private fun populateUiWithExpenseData() {
        etDescription.setText(expense.description)
        val amount = expense.amount
        if (amount == amount.toLong().toDouble()) {
            etAmount.setText(String.format(Locale.getDefault(), "$%.0f", amount))
        } else {
            etAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount))
        }
        btnDate.text = formatDateShortForm(expense.date)

        when (expense.paymentMethod) {
            PaymentMethod.CASH -> radioCash.isChecked = true
            PaymentMethod.CARD -> radioCard.isChecked = true
            PaymentMethod.UNKNOWN -> {
                radioCash.isChecked = false
                radioCard.isChecked = false
            }
        }
        categoryImageView.setImageResource(getCategoryIconResId(expense.category))
    }

    private fun getCategoryIconResId(categoryType: CategoryType): Int {
        return when (categoryType) {
            CategoryType.LIVING -> R.drawable.icon_category_living
            CategoryType.RECREATION -> R.drawable.icon_category_recreation
            CategoryType.TRANSPORT -> R.drawable.icon_category_transport
            CategoryType.FOOD -> R.drawable.icon_category_food
            CategoryType.HEALTH -> R.drawable.icon_category_health
            CategoryType.OTHER -> R.drawable.icon_category_other
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener { attemptSaveExpense() }
        btnDate.setOnClickListener { openDatePicker() }
        btnDelete.setOnClickListener { attemptDeleteExpense() }
    }

    private fun openDatePicker() {
        showDatePicker(
            context = requireContext(),
            initialTimestamp = selectedTimestamp ?: expense.date,
            onDateSelected = { timestampMillis ->
                selectedTimestamp = timestampMillis
                btnDate.text = formatDateLongForm(selectedTimestamp!!)
            }
        )
    }

    private fun attemptSaveExpense() {
        val (isValid, updatedDescription, updatedAmount) = validateInputs()
        if (!isValid) return

        val selectedPaymentMethod = when {
            radioCash.isChecked -> PaymentMethod.CASH
            radioCard.isChecked -> PaymentMethod.CARD
            else -> expense.paymentMethod
        }

        expense = expense.copy(
            description = updatedDescription!!,
            amount = updatedAmount!!,
            date = selectedTimestamp!!,
            paymentMethod = selectedPaymentMethod
        )
        executeUpdateExpense()
    }

    private fun validateInputs(): Triple<Boolean, String?, Double?> {
        val updatedDescription = etDescription.text.toString()
        val updatedAmountStr = etAmount.text.toString().replace("$", "")
        val updatedAmount = updatedAmountStr.toDoubleOrNull()

        if (updatedDescription.isBlank()) {
            showToast(requireContext(), "Description cannot be empty")
            return Triple(false, null, null)
        }
        if (updatedAmount == null || updatedAmount <= 0.0) {
            showToast(requireContext(), "Please enter a valid positive amount")
            return Triple(false, updatedDescription, null)
        }
        if (selectedTimestamp == null) {
            showToast(requireContext(), "Please select a date")
            return Triple(false, updatedDescription, updatedAmount)
        }
        if (!radioCash.isChecked && !radioCard.isChecked) {
            showToast(requireContext(), "Please select a payment method")
            return Triple(false, updatedDescription, updatedAmount)
        }
        return Triple(true, updatedDescription, updatedAmount)
    }

    private fun executeUpdateExpense() {
        val expenseRepository = ExpenseRepository()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                expenseRepository.updateExpense(
                    expense,
                    onSuccess = {
                        launch(Dispatchers.Main) {
                            Log.d("UPDATE_SUCCESS", "Expense updated successfully")
                            showToast(requireContext(), "Expense updated successfully")
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY_UPDATE_EXPENSE,
                                bundleOf(BUNDLE_KEY_UPDATED to true)
                            )
                            sharedViewModel.notifyMovementDataChanged()
                            dismiss()
                        }
                    },
                    onFailure = { exception ->
                        launch(Dispatchers.Main) {
                            showToast(
                                requireContext(),
                                "Failed to update expense: ${exception.message}"
                            )
                        }
                    }
                )
            }
        }
    }

    private fun attemptDeleteExpense() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                executeDeleteExpense()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun executeDeleteExpense() {
        if (expense.id.isBlank()) {
            showToast(requireContext(), "Cannot delete expense with invalid ID.")
            return
        }

        val expenseRepository = ExpenseRepository()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                expenseRepository.deleteExpense(
                    expense.id,
                    onSuccess = {
                        launch(Dispatchers.Main) {
                            Log.d("DELETE_SUCCESS", "Expense deleted successfully")
                            showToast(requireContext(), "Expense deleted successfully")
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY_DELETE_EXPENSE,
                                bundleOf(BUNDLE_KEY_DELETED to true)
                            )
                            sharedViewModel.notifyMovementDataChanged()
                            dismiss()
                        }
                    },
                    onFailure = { exception ->
                        launch(Dispatchers.Main) {
                            showToast(
                                requireContext(),
                                "Failed to delete expense: ${exception.message}"
                            )
                        }
                    }
                )
            }
        }
    }

    private fun setRadioColors() {
        ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)?.let {
            radioCash.buttonTintList = it
        }
        ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)?.let {
            radioCard.buttonTintList = it
        }
    }
}