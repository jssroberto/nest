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
            textView.setTextColor(ContextCompat.getColor(context, R.color.category_spinner))
            textView.setOnClickListener {
                handleSelection(textView, categoryName)
            }
        }
    }

    private fun handleSelection(textView: TextView, categoryName: String) {
        selectedCategoryName = if (selectedCategoryName == categoryName) {
            selectedCategoryName = null
            updateTextViewColors(null)
            onCategorySelected(null)
            null
        } else {
            selectedCategoryName = categoryName
            updateTextViewColors(categoryName)
            onCategorySelected(categoryName)
            categoryName
        }
    }

    fun clearSelections() {
        selectedCategoryName = null
        updateTextViewColors(null)
    }

    private fun updateTextViewColors(selectedName: String?) {
        val colorDefault = ContextCompat.getColor(context, R.color.category_spinner)
        val colorHint = ContextCompat.getColor(context, R.color.txt_hint)
        val colorSelected = ContextCompat.getColor(context, R.color.category_spinner)

        categoryTextViews.forEach { textView ->
            val categoryName = textView.tag as? String ?: return@forEach
            val percentage = categories.find { it.type.displayName == categoryName }?.percentage ?: 0.0f

            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

            if (selectedName == null) {
                textView.setTextColor(colorDefault)
                textView.text = categoryName
            } else if (selectedName == categoryName) {
                textView.setTextColor(colorSelected) // 👈 Color al estar seleccionada
                textView.text = "$categoryName  ${"%.1f".format(percentage)}%"
            } else {
                textView.setTextColor(colorHint)
                textView.text = categoryName
            }
        }
    }


}
