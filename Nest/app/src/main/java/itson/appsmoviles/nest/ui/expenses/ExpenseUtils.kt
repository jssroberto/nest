package itson.appsmoviles.nest.ui.expenses

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Category
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable
import itson.appsmoviles.nest.ui.expenses.manager.CategorySelectionManager


fun calculateCategorySums(expenses: List<Expense>): Map<String, Float> {
    return expenses.groupBy { it.category.displayName }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }
}

fun calculateTotal(expenses: List<Expense>): Float {
    return expenses.sumOf { it.amount }.toFloat()
}


@SuppressLint("ClickableViewAccessibility")
fun configure(
    context: Context,
    graphView: View,
    categories: List<Category>,
    categoryTextViews: List<TextView>,
    onCategorySelected: (String?) -> Unit
): PieChartDrawable {
    val drawable = PieChartDrawable(context, categories, categoryTextViews, onCategorySelected)
    graphView.background = drawable
    graphView.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            drawable.handleTouch(event.x, event.y)
        }
        true
    }
    return drawable
}


fun setup(
    context: Context,
    view: View,
    categories: List<Category>,
    onCategorySelected: (String?) -> Unit
): Pair<List<TextView>, CategorySelectionManager> {
    val ids = listOf(
        R.id.homeTextView, R.id.recreationTextView,
        R.id.transportTextView, R.id.foodTextView,
        R.id.healthTextView, R.id.othersTextView
    )

    val textViews = ids.mapIndexed { index, id ->
        view.findViewById<TextView>(id).apply {
            tag = categories[index].type.displayName
        }
    }

    val manager = CategorySelectionManager(context, textViews, categories) { selectedName ->
        onCategorySelected(selectedName)
    }
    manager.setup()

    return Pair(textViews, manager)
}




