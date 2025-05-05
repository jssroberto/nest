package itson.appsmoviles.nest.ui.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType


fun setUpSpinner(context: Context, spinner: Spinner) {
    val hint = "Select a Category"
    val actualCategories = CategoryType.entries.map { it.name.toTitleCase() }
    val itemsWithHint = listOf(hint) + actualCategories

    val adapter = object : ArrayAdapter<String>(context, R.layout.spinner_item, itemsWithHint) {
        override fun isEnabled(position: Int): Boolean {
            return position != 0
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            if (position == 0) {
                view.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
            } else {
                view.setTextColor(ContextCompat.getColor(context, R.color.txt_color))
            }
            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            if (position == 0) {
                view.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
            }
            return view
        }
    }

    adapter.setDropDownViewResource(R.layout.spinner_item)
    spinner.adapter = adapter
}

