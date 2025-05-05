package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.Spinner
import androidx.annotation.RequiresApi
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.showDatePicker

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
            showDatePicker(context) { timestamp ->
                startTimestamp = timestamp
                startDateButton.text = formatDateShortForm(timestamp)
            }
        }
        endDateButton.setOnClickListener {
            showDatePicker(context) { timestamp ->
                endTimestamp = timestamp
                endDateButton.text = formatDateShortForm(timestamp)
            }
        }
    }

    fun clearFilters() {
        startTimestamp = null
        endTimestamp = null
        spinner.setSelection(0)
        startDateButton.text = context.getString(R.string.start_date)
        endDateButton.text = context.getString(R.string.end_date)
    }

    fun getSelectedCategory(): String? {
        val selected = spinner.selectedItem?.toString()
        return if (selected.isNullOrBlank() || selected.equals("Select a category", ignoreCase = true)) null else selected
    }
}
