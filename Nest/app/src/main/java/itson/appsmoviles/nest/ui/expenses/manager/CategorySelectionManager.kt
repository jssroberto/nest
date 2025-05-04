package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Category

class CategorySelectionManager(
    private val context: Context,
    private val categoryTextViews: List<TextView>,
    private val categories: List<Category>,
    private val onCategorySelected: (String?) -> Unit
) {
    private var selectedCategoryName: String? = null

    fun setup() {
        categoryTextViews.forEach { textView ->
            val categoryName = textView.tag as? String ?: return@forEach
            textView.setTextColor(ContextCompat.getColor(context, R.color.black))
            textView.setOnClickListener {
                handleSelection(textView, categoryName)
            }
        }
    }

    private fun handleSelection(textView: TextView, categoryName: String) {
        selectedCategoryName = if (selectedCategoryName == categoryName) {
            clearSelections()
            onCategorySelected(null)
            null
        } else {
            highlight(textView, categoryName)
            onCategorySelected(categoryName)
            categoryName
        }
    }

    fun clearSelections() {
        categoryTextViews.forEach {
            it.setTextColor(ContextCompat.getColor(context, R.color.black))
            it.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            it.text = it.tag?.toString() ?: ""
        }
    }

    private fun highlight(textView: TextView, categoryName: String) {
        clearSelections()
        val percentage = categories.find { it.type.displayName == categoryName }?.percentage ?: 0.0f
        textView.setTextColor(ContextCompat.getColor(context, R.color.black))
        textView.text = "$categoryName  ${"%.1f".format(percentage)}%"
    }
}
