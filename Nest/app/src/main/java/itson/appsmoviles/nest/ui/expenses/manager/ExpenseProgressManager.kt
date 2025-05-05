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
            val hasExpense = total > 0f

            // 🔁 Nombre visible (traducido o amigable)
            val displayName = when (categoryKey.uppercase()) {
                "FOOD" -> context.getString(R.string.food)
                "TRANSPORT" -> context.getString(R.string.transport)
                "HEALTH" -> context.getString(R.string.health)
                "HOME" -> context.getString(R.string.home)
                "RECREATION" -> context.getString(R.string.recreation) // Ajusta según tu strings.xml
                "OTHERS" -> context.getString(R.string.other)
                else -> categoryKey
            }

            // 🏷️ Texto del nombre de la categoría
            val label = TextView(context).apply {
                text = displayName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTextColor(ContextCompat.getColor(context, R.color.black))
                setPadding(0, 24, 0, 8)
            }

            // 📊 Barra personalizada con animación
            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    78
                )
                background = ExpensesDrawable(
                    total = target,
                    current = total,
                    currentColor = ContextCompat.getColor(context, R.color.txt_income),
                    totalColor = ContextCompat.getColor(context, R.color.txt_color),
                    typeface = ResourcesCompat.getFont(context, R.font.lexend_bold),
                )
            }

            container.addView(label)
            container.addView(bar)
        }
    }


}
