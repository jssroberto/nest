package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import itson.appsmoviles.nest.R


class PercentageBudgetFragment : Fragment() {
    private lateinit var editTextAmountFood: EditText
    private lateinit var editTextAmountHome: EditText
    private lateinit var editTextAmountRecreation: EditText
    private lateinit var editTextAmountHealth: EditText
    private lateinit var editTextAmountTransport: EditText
    private lateinit var editTextAmountOthers: EditText
    private val editTexts = mutableListOf<EditText>()
    private var isProgrammaticChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_percentage_budget, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextAmountFood = view.findViewById(R.id.et_food)
        editTextAmountHome = view.findViewById(R.id.et_home)
        editTextAmountHealth = view.findViewById(R.id.et_health)
        editTextAmountRecreation = view.findViewById(R.id.et_recreation)
        editTextAmountTransport = view.findViewById(R.id.et_transport)
        editTextAmountOthers = view.findViewById(R.id.et_others)
        val editTextBudget: EditText = view.findViewById(R.id.monthly_budget)

        editTexts.addAll(listOf(
            editTextAmountFood,
            editTextAmountHome,
            editTextAmountHealth,
            editTextAmountRecreation,
            editTextAmountTransport,
            editTextAmountOthers))

        editTexts.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    if (!isProgrammaticChange) {
                        handleTextChange(editText, editable)
                    }
                }
            })
        }

    }

    private fun handleTextChange(currentEditText: EditText, editable: Editable?) {
        isProgrammaticChange = true

        val input = editable?.toString()?.replace(Regex("[^\\d]"), "") ?: ""
        var value = input.toIntOrNull() ?: 0

        // Individual maximum
        value = value.coerceAtMost(100)

        // Calculate sum of other fields
        val sumOthers = editTexts
            .filter { it != currentEditText }
            .sumOf {
                it.text.toString().replace(Regex("[^\\d]"), "").toIntOrNull() ?: 0
            }

        // Total maximum
        val maxAllowed = (100 - sumOthers).coerceAtLeast(0)
        value = value.coerceAtMost(maxAllowed)

        // Update text
        val newText = "$value%"
        currentEditText.setText(newText)
        currentEditText.setSelection(newText.length - 1)

        isProgrammaticChange = false
    }


}