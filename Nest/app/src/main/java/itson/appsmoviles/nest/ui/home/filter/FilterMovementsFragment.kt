package itson.appsmoviles.nest.ui.home.filter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast

@RequiresApi(Build.VERSION_CODES.O)
class FilterMovementsFragment : DialogFragment() {
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var spinner: Spinner
    private lateinit var btnApplyFilters: Button
    private lateinit var btnClearFilters: ImageButton
    private var startSelectedTimestamp: Long? = null
    private var endSelectedTimestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        spinner = view.findViewById(R.id.spinner_categories_filter)
        btnApplyFilters = view.findViewById(R.id.btn_apply_filters)
        btnClearFilters = view.findViewById(R.id.btn_clear_filters)

        setUpSpinner(requireContext(), spinner)
        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        btnStartDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
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
                onDateSelected = { timestampMillis ->
                    endSelectedTimestamp = timestampMillis
                    btnEndDate.apply {
                        text = formatDateShortForm(timestampMillis)
                    }
                }
            )
        }

        btnApplyFilters.setOnClickListener {

        }

        btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun clearFilters() {
        btnStartDate.text = ""
        btnEndDate.text = ""
        btnStartDate.hint = getString(R.string.start_date)
        btnEndDate.hint = getString(R.string.end_date)
        startSelectedTimestamp = null
        endSelectedTimestamp = null
        spinner.setSelection(0)
    }

}