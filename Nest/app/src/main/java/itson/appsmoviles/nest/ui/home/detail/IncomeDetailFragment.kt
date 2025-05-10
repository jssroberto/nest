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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.data.repository.IncomeRepository
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.getValue

@RequiresApi(Build.VERSION_CODES.O)
class IncomeDetailFragment : DialogFragment() {

    private val sharedViewModel: SharedMovementsViewModel by activityViewModels()

    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: ImageButton

    private var selectedTimestamp: Long? = null
    private lateinit var income: Income

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_AMOUNT = "amount"
        private const val ARG_DATE = "date"

        const val REQUEST_KEY_UPDATE_INCOME = "update_income_result"
        const val REQUEST_KEY_DELETE_INCOME = "delete_income_result"
        const val BUNDLE_KEY_UPDATED = "updated"
        const val BUNDLE_KEY_DELETED = "deleted"

        fun newInstance(income: Income): IncomeDetailFragment {
            return IncomeDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, income.id)
                    putString(ARG_DESCRIPTION, income.description)
                    putDouble(ARG_AMOUNT, income.amount)
                    putLong(ARG_DATE, income.date)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent) // Or your custom background
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure you have a layout file named fragment_income_detail.xml
        return inflater.inflate(R.layout.fragment_income_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadIncomeFromArguments()
        populateUiWithIncomeData()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        etDescription = view.findViewById(R.id.et_description_income_detail)
        etAmount = view.findViewById(R.id.et_amount_income_detail)
        btnDate = view.findViewById(R.id.btn_date_income_detail)
        btnSave = view.findViewById(R.id.btn_save_income_detail)
        btnDelete = view.findViewById(R.id.btn_delete_income_detail)
    }

    private fun loadIncomeFromArguments() {
        val args = arguments ?: return

        income = Income(
            id = args.getString(ARG_ID) ?: "",
            description = args.getString(ARG_DESCRIPTION) ?: "",
            amount = args.getDouble(ARG_AMOUNT, 0.0),
            date = args.getLong(ARG_DATE, 0L)
        )
        selectedTimestamp = income.date
    }

    private fun populateUiWithIncomeData() {
        etDescription.setText(income.description)
        val amount = income.amount
        // Format amount to show whole number if no decimal part, or 2 decimal places otherwise
        if (amount == amount.toLong().toDouble()) {
            etAmount.setText(String.format(Locale.getDefault(), "$%.0f", amount))
        } else {
            etAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount))
        }
        btnDate.text = formatDateShortForm(income.date)
    }

    private fun setupListeners() {
        btnSave.setOnClickListener { attemptSaveIncome() }
        btnDate.setOnClickListener { openDatePicker() }
        btnDelete.setOnClickListener { attemptDeleteIncome() }
    }

    private fun openDatePicker() {
        showDatePicker(
            context = requireContext(),
            initialTimestamp = selectedTimestamp ?: income.date,
            onDateSelected = { timestampMillis ->
                selectedTimestamp = timestampMillis
                btnDate.text = formatDateLongForm(selectedTimestamp!!)
            }
        )
    }

    private fun attemptSaveIncome() {
        val (isValid, updatedDescription, updatedAmount) = validateInputs()
        if (!isValid) return

        income = income.copy(
            description = updatedDescription!!,
            amount = updatedAmount!!,
            date = selectedTimestamp!!
        )
        executeUpdateIncome()
    }

    private fun validateInputs(): Triple<Boolean, String?, Double?> {
        val updatedDescription = etDescription.text.toString().trim()
        val updatedAmountStr = etAmount.text.toString().replace("$", "").trim()
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
        return Triple(true, updatedDescription, updatedAmount)
    }

    private fun executeUpdateIncome() {
        val incomeRepository = IncomeRepository() // Ensure you have an IncomeRepository

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                incomeRepository.updateIncome(
                    income,
                    onSuccess = {
                        launch(Dispatchers.Main) {
                            Log.d("UPDATE_SUCCESS", "Income updated successfully")
                            showToast(requireContext(), "Income updated successfully")
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY_UPDATE_INCOME,
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
                                "Failed to update income: ${exception.message}"
                            )
                        }
                    }
                )
            }
        }
    }

    private fun attemptDeleteIncome() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this income? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                executeDeleteIncome()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun executeDeleteIncome() {
        if (income.id.isBlank()) {
            showToast(requireContext(), "Cannot delete income with invalid ID.")
            return
        }

        val incomeRepository = IncomeRepository() // Ensure you have an IncomeRepository

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                incomeRepository.deleteIncome(
                    income.id,
                    onSuccess = {
                        launch(Dispatchers.Main) {
                            Log.d("DELETE_SUCCESS", "Income deleted successfully")
                            showToast(requireContext(), "Income deleted successfully")
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY_DELETE_INCOME,
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
                                "Failed to delete income: ${exception.message}"
                            )
                        }
                    }
                )
            }
        }
    }
}