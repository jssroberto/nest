package itson.appsmoviles.nest.ui.add.income

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.ui.add.AddFragment
import itson.appsmoviles.nest.ui.home.HomeFragment
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.addDollarSign
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

@RequiresApi(Build.VERSION_CODES.O)
class AddIncomeFragment : Fragment() {

    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnAdd: Button
    private lateinit var viewModel: AddIncomeViewModel

    private val sharedViewModel: SharedMovementsViewModel by activityViewModels()
    private var selectedTimestamp: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_income, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtAmount = view.findViewById(R.id.edt_amount_income)
        edtDescription = view.findViewById(R.id.edt_description_income)
        btnDate = view.findViewById(R.id.btn_date_income)
        btnAdd = view.findViewById(R.id.btn_add_income)
        viewModel = ViewModelProvider(this)[AddIncomeViewModel::class.java]

        setupAmountFormatting()
        setupListeners()
    }

    private fun setupListeners() {
        btnDate.setOnClickListener {
            showDatePicker(requireContext()) { timestamp ->
                selectedTimestamp = timestamp
                btnDate.text = formatDateLongForm(timestamp)
            }
        }

        btnAdd.setOnClickListener {
            val rawAmountText = edtAmount.text.toString()
                .replace("$", "")
                .replace(",", "")
                .replace("[^\\d.]".toRegex(), "")

            val description = edtDescription.text.toString().trim()

            if (rawAmountText.isBlank() || description.isBlank() || selectedTimestamp == null) {
                Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = rawAmountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount >= 10_000_000) {
                Toast.makeText(requireContext(), "Max amount is 9,999,999.99", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val income = Income(
                id = "",
                description = description,
                amount = String.format(Locale.US, "%.2f", amount).toDouble(),
                date = selectedTimestamp ?: System.currentTimeMillis()
            )

            viewModel.addIncome(income,
                onSuccess = {
                    sharedViewModel.notifyMovementDataChanged()

                    parentFragmentManager.popBackStack()
                    navigateToHome()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun navigateToHome() {
        requireActivity().supportFragmentManager.popBackStack(
            AddFragment.TAG,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

        private fun setupAmountFormatting() {
        val maxBefore = 7
        val maxAfter = 2

        edtAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.")

        edtAmount.filters = arrayOf(InputFilter.LengthFilter(maxBefore + 1 + maxAfter))

        edtAmount.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || !edtAmount.hasFocus()) {
                    return
                }
                isFormatting = true

                var cleanString = s.toString().replace("[^\\d.]".toRegex(), "")

                val firstDotIndex = cleanString.indexOf('.')
                if (firstDotIndex != -1) {
                    val beforeDot = cleanString.substring(0, firstDotIndex + 1)
                    val afterDot = cleanString.substring(firstDotIndex + 1).replace(".", "")
                    cleanString = beforeDot + afterDot
                }

                val parts = cleanString.split('.', limit = 2)
                var integerPart = parts.getOrNull(0) ?: ""
                val decimalPart = parts.getOrNull(1) ?: ""

                if (integerPart.length > 1 && integerPart.startsWith('0') && !cleanString.startsWith("0.")) { // "007" -> "7"
                    integerPart = integerPart.trimStart('0')
                    if (integerPart.isEmpty()) integerPart = "0"
                }
                if (integerPart.isEmpty() && cleanString.startsWith(".")) {
                    integerPart = "0"
                }

                integerPart = integerPart.take(maxBefore)
                val finalDecimalPart = decimalPart.take(maxAfter)



                val formattedText = when {
                    cleanString.endsWith(".") -> "$integerPart."
                    finalDecimalPart.isNotEmpty() -> "$integerPart.$finalDecimalPart"
                    integerPart.isNotEmpty() -> integerPart
                    else -> ""
                }

                if (s.toString() != formattedText) {
                    edtAmount.setText(formattedText)
                    edtAmount.setSelection(formattedText.length)
                }

                isFormatting = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        edtAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                var text = edtAmount.text.toString()
                text = text.replace("$", "").replace(",", "")
                edtAmount.setText(text)
                if (text.isNotEmpty()) {
                    edtAmount.setSelection(text.length)
                }
            } else {
                var rawText = edtAmount.text.toString().replace("[^\\d.]".toRegex(), "")

                if (rawText.endsWith(".")) {
                    rawText = rawText.substring(0, rawText.length - 1)
                }

                val amount = rawText.toDoubleOrNull()
                if (amount != null) {
                    val formatted = String.format(Locale.US, "$%,.2f", amount)
                    edtAmount.setText(formatted)
                } else if (edtAmount.text.toString().isNotEmpty() && edtAmount.text.toString() != "$") {
                }
            }
        }
    }
}