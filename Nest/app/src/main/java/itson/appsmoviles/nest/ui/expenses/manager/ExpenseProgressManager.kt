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

class ExpenseProgressManager(private val context: Context) {

    private val colorMapping = mapOf(
        "Food" to R.color.category_food,
        "Transport" to R.color.category_transport,
        "Health" to R.color.category_health,
        "Others" to R.color.category_other,
        "Home" to R.color.category_living,
        "Recreation" to R.color.category_recreation
    )

    fun updateProgressBars(
        rootView: View,
        expenseSums: Map<String, Float>,
        targets: Map<String, Float>
    ) {
        val container = rootView.findViewById<LinearLayout>(R.id.progressContainer)
        container.removeAllViews()

        val sortedCategories = colorMapping.keys.sortedByDescending { (expenseSums[it] ?: 0f) > 0f }

        for (categoryKey in sortedCategories) {
            val total = expenseSums[categoryKey] ?: 0f
            val target = targets[categoryKey] ?: 1f
            val isZeroExpense = total <= 0f

            val displayName = when (categoryKey.uppercase()) {
                "FOOD" -> context.getString(R.string.food)
                "TRANSPORT" -> context.getString(R.string.transport)
                "HEALTH" -> context.getString(R.string.health)
                "HOME" -> context.getString(R.string.home)
                "RECREATION" -> context.getString(R.string.recreation)
                "OTHERS" -> context.getString(R.string.other)
                else -> categoryKey
            }
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
