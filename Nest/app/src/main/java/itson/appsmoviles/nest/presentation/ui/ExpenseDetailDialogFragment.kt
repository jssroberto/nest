package itson.appsmoviles.nest.presentation.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.domain.model.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class ExpenseDetailDialogFragment : DialogFragment() {
    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var categoryImageView: ImageView
    private lateinit var btnSave: Button

    private lateinit var expense: Expense

    companion object {
        fun newInstance(expense: Expense): ExpenseDetailDialogFragment {
            return ExpenseDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("description", expense.description)
                    putFloat("amount", expense.amount)
                    putString("date", expense.date)
                    putString("category", expense.category.name)
                    putString("id", expense.id)
                    putString("paymentMethod", expense.paymentMethod)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
            val category = Category.values().find { it.name == categoryName } ?: Category.OTHER

            expense = Expense(
                id = getString("id") ?: "",
                description = getString("description") ?: "",
                amount = getFloat("amount", 0f),
                date = getString("date") ?: "",
                category = category,
                paymentMethod = getString("paymentMethod") ?: "UNKNOWN"
            )
        }

        etDescription.setText(expense.description)
        etAmount.setText(expense.amount.toString())
        etDate.setText(expense.date)

        val iconResId = when (expense.category) {
            Category.LIVING -> R.drawable.icon_category_living
            Category.RECREATION -> R.drawable.icon_category_recreation
            Category.TRANSPORT -> R.drawable.icon_category_transport
            Category.FOOD -> R.drawable.icon_category_food
            Category.HEALTH -> R.drawable.icon_category_health
            Category.OTHER -> R.drawable.icon_category_other
        }

        categoryImageView.setImageResource(iconResId)

        btnSave.setOnClickListener { saveExpense() }
    }

    private fun saveExpense() {
        val updatedDescription = etDescription.text.toString()
        val updatedAmount = etAmount.text.toString().toFloatOrNull() ?: 0.0f
        val updatedDate = etDate.text.toString()

        if (updatedDescription.isNotEmpty() && updatedAmount > 0 && updatedDate.isNotEmpty()) {
            expense = expense.copy(description = updatedDescription, amount = updatedAmount, date = updatedDate)
            updateExpenseInDatabase(expense)
            dismiss()
        } else {
            Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateExpenseInDatabase(expense: Expense) {
        val expenseRepository = ExpenseRepository()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                expenseRepository.updateExpense(
                    expense.id,
                    expense.amount.toDouble(),
                    expense.description,
                    expense.category,
                    expense.paymentMethod,
                    expense.date,
                    onSuccess = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Expense updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error updating expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error updating expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
