package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.expenses.drawable.ExpensesDrawable

class ExpenseProgressManager(private val context: Context) {

    private val progressMapping = mapOf(
        "Food" to R.id.foodBudget,
        "Transport" to R.id.transportBudget,
        "Health" to R.id.budgetHealth,
        "Others" to R.id.budgetOthers,
        "Home" to R.id.budgetHome,
        "Recreation" to R.id.budgetRecreation
    )

    fun updateProgressBars(
        rootView: View,
        expenseSums: Map<String, Float>,
        targets: Map<String, Float>
    ) {
        for ((category, viewId) in progressMapping) {
            val total = expenseSums[category] ?: 0f
            val target = targets[category] ?: 1f
            updateSingleBar(rootView.findViewById(viewId), total, target)
        }
    }

    private fun updateSingleBar(view: View, total: Float, target: Float) {
        val progressColor = ContextCompat.getColor(context, R.color.primary_color)
        val backgroundColor = ContextCompat.getColor(context, R.color.txt_income)
        val drawable = ExpensesDrawable(total, target, progressColor, backgroundColor)
        view.background = drawable
    }
}
