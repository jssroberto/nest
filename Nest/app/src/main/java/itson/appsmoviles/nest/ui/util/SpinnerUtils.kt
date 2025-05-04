package itson.appsmoviles.nest.ui.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType

object SpinnerUtils {

    fun setUpSpinner(context: Context, spinner: Spinner) {
        val hint = "Select a Category"
        val actualCategories = CategoryType.entries.map { it.name.toTitleCase() }

        val adapter = object :
            ArrayAdapter<String>(context, R.layout.spinner_item, actualCategories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (spinner.selectedItemPosition == 0) {
                    (view as TextView).text = hint
                    view.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
    }

    fun clearFilters(
        spinner: Spinner,
        startDateButton: Button,
        endDateButton: Button,
        context: Context
    ) {

        spinner.setSelection(0)

        startDateButton.text = context.getString(R.string.start_date)
        endDateButton.text = context.getString(R.string.end_date)

    }

}
