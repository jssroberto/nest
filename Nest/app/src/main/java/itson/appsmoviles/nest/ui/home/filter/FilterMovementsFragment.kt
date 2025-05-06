package itson.appsmoviles.nest.ui.home.filter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.setUpTypesSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker


@RequiresApi(Build.VERSION_CODES.O)
class FilterMovementsFragment : DialogFragment() {
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var tvByType: TextView
    private lateinit var tvByCategory: TextView
    private lateinit var tvByDate: TextView
    private lateinit var spinnerCategories: Spinner
    private lateinit var spinnerTypes: Spinner
    private lateinit var btnApplyFilters: Button
    private lateinit var btnClearFilters: ImageButton
    private var startSelectedTimestamp: Long? = null
    private var endSelectedTimestamp: Long? = null

    private val movementTypes = listOf("Incomes", "Expenses")


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_movements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnStartDate = view.findViewById(R.id.btn_start_date_filter)
        btnEndDate = view.findViewById(R.id.btn_end_date_filter)
        spinnerCategories = view.findViewById(R.id.spinner_categories_filter)
        spinnerTypes = view.findViewById(R.id.spinner_types_filter)
        btnApplyFilters = view.findViewById(R.id.btn_apply_filters)
        btnClearFilters = view.findViewById(R.id.btn_clear_filters)
        tvByType = view.findViewById(R.id.tv_by_type_filter)
        tvByCategory = view.findViewById(R.id.tv_by_category_filter)
        tvByDate = view.findViewById(R.id.tv_by_date_filter)

        setUpSpinner(requireContext(), spinnerCategories)
        setUpTypesSpinner(requireContext(), spinnerTypes)

        setUpClickListeners()
        setUpSpinnerListeners()
    }


    private fun setUpClickListeners() {
        btnStartDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                maxTimestamp = endSelectedTimestamp ?: System.currentTimeMillis(),
                onDateSelected = { timestampMillis ->
                    startSelectedTimestamp = timestampMillis
                    btnStartDate.apply {
                        text = formatDateShortForm(timestampMillis)
                    }
                }
            )
        }

        btnEndDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                minTimestamp = startSelectedTimestamp ?: 0,
                onDateSelected = { timestampMillis ->
                    endSelectedTimestamp = timestampMillis
                    btnEndDate.apply {
                        text = formatDateShortForm(timestampMillis)
                    }
                }
            )
        }

        btnApplyFilters.setOnClickListener {
            // TODO: Implement filter logic
            dismiss()
        }

        btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setUpSpinnerListeners() {
        spinnerTypes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedType = parent?.getItemAtPosition(position).toString()
                if (selectedType == movementTypes[0]) {
                    tvByCategory.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.txt_hint
                        )
                    )
                    spinnerCategories.isEnabled = false
                    spinnerCategories.alpha = 0.5f
                    spinnerCategories.setSelection(0)

                } else {
                    tvByCategory.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.txt_color
                        )
                    )
                    spinnerCategories.isEnabled = true
                    spinnerCategories.alpha = 1.0f
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinnerCategories.isEnabled = true
                spinnerCategories.alpha = 1.0f
            }
        }

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val typesAdapter = spinnerTypes.adapter as ArrayAdapter<String>
                    val expensePosition =
                        typesAdapter.getPosition(movementTypes[1])

                    if (expensePosition >= 0 && spinnerTypes.selectedItemPosition != expensePosition) {
                        spinnerTypes.setSelection(expensePosition)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }


    private fun clearFilters() {
        btnStartDate.text = ""
        btnEndDate.text = ""
        btnStartDate.hint = getString(R.string.start_date)
        btnEndDate.hint = getString(R.string.end_date)
        startSelectedTimestamp = null
        endSelectedTimestamp = null

        spinnerCategories.setSelection(0)
        spinnerTypes.setSelection(0)

        spinnerCategories.isEnabled = true
        spinnerCategories.alpha = 1.0f

        tvByCategory.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.txt_color
            )
        )
    }

}