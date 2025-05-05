package itson.appsmoviles.nest.ui.home.detail

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class ExpenseDetailDialogFragment : DialogFragment() {

    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var categoryImageView: ImageView
    private lateinit var btnSave: Button
    private var selectedTimestamp: Long? = null

    private lateinit var expense: Expense

    companion object {
        fun newInstance(expense: Expense): ExpenseDetailDialogFragment {
            return ExpenseDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("description", expense.description)
                    putDouble("amount", expense.amount)
                    putLong("date", expense.date)
                    putString("category", expense.category.name)
                    putString("id", expense.id)
                    putString("paymentMethod", expense.paymentMethod.name)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_expense_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDescription = view.findViewById(R.id.et_description)
        etAmount = view.findViewById(R.id.et_amount)
        etDate = view.findViewById(R.id.et_date)
        categoryImageView = view.findViewById(R.id.iv_icon)
        btnSave = view.findViewById(R.id.btn_save)

        arguments?.run {
            val categoryName = getString("category") ?: "OTHER"
            val categoryType =
                CategoryType.entries.find { it.name == categoryName } ?: CategoryType.OTHER

            val paymentMethodName = getString("paymentMethod")

            val paymentMethodType = paymentMethodName?.let { name ->
                PaymentMethod.entries.find { it.name.equals(name, ignoreCase = true) }
            } ?: PaymentMethod.CARD

            expense = Expense(
                id = getString("id") ?: "",
                description = getString("description") ?: "",
                amount = getDouble("amount", 0.0),
                date = getLong("date", 0L),
                category = categoryType,
                paymentMethod = paymentMethodType
            )
        }

        selectedTimestamp = expense.date

        etDescription.setText(expense.description)
        etAmount.setText(expense.amount.toString())
        etDate.setText(formatDateLongForm(expense.date)) // Display the initial date

        val iconResId = when (expense.category) {
            CategoryType.LIVING -> R.drawable.icon_category_living
            CategoryType.RECREATION -> R.drawable.icon_category_recreation
            CategoryType.TRANSPORT -> R.drawable.icon_category_transport
            CategoryType.FOOD -> R.drawable.icon_category_food
            CategoryType.HEALTH -> R.drawable.icon_category_health
            CategoryType.OTHER -> R.drawable.icon_category_other
        }

        categoryImageView.setImageResource(iconResId)

        btnSave.setOnClickListener { saveExpense() }

        etDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                initialTimestamp = expense.date,
                onDateSelected = { timestampMillis ->
                    selectedTimestamp = timestampMillis
                    etDate.setText(formatDateLongForm(selectedTimestamp!!))
                }
            )
        }
    }

    private fun saveExpense() {
        val updatedDescription = etDescription.text.toString()
        val updatedAmount = etAmount.text.toString().toDoubleOrNull()

        if (updatedDescription.isBlank()) {
            showToast(requireContext(), "Description cannot be empty")
            return
        }
        if (updatedAmount == null || updatedAmount <= 0.0) {
            showToast(requireContext(), "Please enter a valid positive amount")
            return
        }
        if (selectedTimestamp == null) {
            showToast(requireContext(), "Please select a date")
            return
        }

        expense = expense.copy(
            description = updatedDescription,
            amount = updatedAmount,
            date = selectedTimestamp!!
        )
        updateExpense(expense)
    }


    private fun updateExpense(expense: Expense) {
        val expenseRepository = ExpenseRepository()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                expenseRepository.updateExpense(
                    expense,
                    onSuccess = {
                        launch(Dispatchers.Main) {
                            Log.d("UPDATE_SUCCESS", "Expense updated successfully")
                            parentFragmentManager.setFragmentResult(
                                "update_expense_result",
                                bundleOf("updated" to true)
                            )
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
}