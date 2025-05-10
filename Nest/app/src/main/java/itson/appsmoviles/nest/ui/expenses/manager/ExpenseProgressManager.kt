package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.util.TypedValue
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.expenses.drawable.ExpensesDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import itson.appsmoviles.nest.data.enum.CategoryType

class ExpenseProgressManager(private val context: Context) {

    fun updateProgressBars(
        rootView: View,
        expenseSums: Map<CategoryType, Float>,
        targets: Map<CategoryType, Float>
    ) {
        val container = rootView.findViewById<LinearLayout>(R.id.progressContainer)
        container.removeAllViews()

        val sortedCategories = targets.keys
            .sortedByDescending { expenseSums[it] ?: 0f }

        for (category in sortedCategories) {
                val total = expenseSums[category] ?: 0f
                val target = targets[category] ?: 1f
                val isZeroExpense = total <= 0f

            val displayName = category.displayName
            val label = TextView(context).apply {
                text = displayName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

                val isOverBudget = total > target
                val labelColor = when {
                    isZeroExpense -> R.color.txt_hint
                    isOverBudget -> R.color.dark_orange
                    else -> R.color.txt_color
                }

                setTextColor(ContextCompat.getColor(context, labelColor))
                setPadding(0, 24, 0, 8)
            }


            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    78
                )
                background = ExpensesDrawable(
                    total = target,
                    current = total,
                    totalColor = ContextCompat.getColor(context, R.color.light_blue),
                    textColor = ContextCompat.getColor(
                        context,
                        if (isZeroExpense) R.color.txt_hint else R.color.off_white
                    ),
                    context = context
                )
            }

            container.addView(label)
            container.addView(bar)
        }
    }

}
