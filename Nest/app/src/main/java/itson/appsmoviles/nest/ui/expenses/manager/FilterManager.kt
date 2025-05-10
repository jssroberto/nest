package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.showDatePicker
import java.time.LocalDate

class FilterManager(
    private val context: Context,
    private val startDateButton: Button,
    private val endDateButton: Button,
    private val spinner: Spinner
) {
    var startTimestamp: Long? = null
    var endTimestamp: Long? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun setup() {
        startDateButton.setOnClickListener {
            showDatePicker(
                context = context,
                maxTimestamp = endTimestamp ?: System.currentTimeMillis(),
                onDateSelected = { timestampMillis ->
                    startTimestamp = timestampMillis
                    startDateButton.text = formatDateShortForm(timestampMillis)
                    startDateButton.setTextColor(ContextCompat.getColor(context, R.color.txt_color))
                }
            )
        }
        endDateButton.setOnClickListener {
            showDatePicker(
                context = context,
                minTimestamp = startTimestamp ?: 0,
                onDateSelected = { timestampMillis ->
                    endTimestamp = timestampMillis
                    endDateButton.text = formatDateShortForm(timestampMillis)
                    endDateButton.setTextColor(ContextCompat.getColor(context, R.color.txt_color))
                }
            )
        }
    }


    fun clearFilters() {
        startTimestamp = null
        endTimestamp = null
        spinner.setSelection(0)

        startDateButton.text = context.getString(R.string.start_date)
        startDateButton.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))

        endDateButton.text = context.getString(R.string.end_date)
        endDateButton.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
    }



    fun getSelectedCategory(): String? {
        val selected = spinner.selectedItem?.toString()
        return if (selected.isNullOrBlank() || selected.equals("Select a category", ignoreCase = true)) null else selected
    }

    fun validateDatesAndToggleButton(startDate: Long?, endDate: Long?, button: Button) {
        if (startDate != null && endDate != null && startDate > endDate) {
            button.isEnabled = false
            button.alpha = 0.5f
        } else {
            button.isEnabled = true
            button.alpha = 1f
        }
    }


}
