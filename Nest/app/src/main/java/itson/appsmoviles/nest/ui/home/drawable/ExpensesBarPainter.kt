package itson.appsmoviles.nest.ui.home.drawable

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.util.getCategoryColors


class ExpensesBarPainter(
    private val context: Context,
    private val expensesBar: LinearLayout,
    // Make totalBudget mutable
    private var totalBudget: Double
) {

    // Keep track of the last expenses map to repaint if budget changes
    private var lastExpensesMap: Map<CategoryType, Double>? = null

    /**
     * Updates the total budget used for calculating bar segment weights.
     * Optionally repaints the bar with the existing expense data using the new budget.
     *
     * @param newBudget The new total budget.
     * @param repaintNow If true, immediately repaint the bar using the last known expense data.
     */
    fun updateBudget(newBudget: Double, repaintNow: Boolean = false) {
        if (newBudget >= 0) { // Basic validation
            this.totalBudget = newBudget
            if (repaintNow && lastExpensesMap != null) {
                paintExpenses(lastExpensesMap!!) // Repaint with the new budget
            }
        }
    }

    fun paintExpenses(expenses: Map<CategoryType, Double>) {
        // Store the latest expenses map
        this.lastExpensesMap = expenses

        // Prevent division by zero or negative budget issues
        if (totalBudget <= 0.0) {
            expensesBar.removeAllViews() // Clear the bar if budget is invalid
            return
        }

        expensesBar.removeAllViews() // Clear previous segments
        val categoryColors =
            getCategoryColors(context) // Assuming this returns Map<CategoryType, String>

        var usedBudget = 0.0
        for ((category, amount) in expenses) {
            // Skip categories with no or negative expense amount
            if (amount <= 0.0) continue

            // Calculate weight relative to the current totalBudget
            val weight = amount / totalBudget
            // Ensure weight is positive before adding the view
            if (weight <= 0.0) continue

            val barSegment = View(context).apply {
                // Use helper or default color if category color is missing/invalid
                val colorString = categoryColors[category]
                val colorInt = try {
                    colorString?.toColorInt() ?: Color.GRAY
                } catch (e: Exception) {
                    Color.GRAY
                }
                setBackgroundColor(colorInt)

                layoutParams = LinearLayout.LayoutParams(
                    0, // Width is 0, weight determines actual width
                    ViewGroup.LayoutParams.MATCH_PARENT, // Height fills the LinearLayout
                    weight.toFloat() // Weight determines proportion
                )
            }
            expensesBar.addView(barSegment)
            usedBudget += amount
        }

        // Add the remaining budget segment
        paintRemainingBudget(usedBudget)
    }

    private fun paintRemainingBudget(usedBudget: Double) {
        // Ensure usedBudget is not negative
        val nonNegativeUsedBudget = maxOf(0.0, usedBudget)

        // No need to paint remaining if budget is invalid
        if (totalBudget <= 0.0) return

        // Calculate remaining budget, ensuring it's not negative
        val remainingBudget = maxOf(0.0, totalBudget - nonNegativeUsedBudget)

        if (remainingBudget > 0) {
            // Calculate weight of the remaining portion
            val weight = remainingBudget / totalBudget
            // Only add if weight is positive
            if (weight <= 0.0) return

            val emptySegment = View(context).apply {
                setBackgroundColor(Color.TRANSPARENT) // Make the unused part transparent
                layoutParams = LinearLayout.LayoutParams(
                    0, // Width 0
                    ViewGroup.LayoutParams.MATCH_PARENT, // Match height
                    weight.toFloat() // Weight determines proportion
                )
            }
            expensesBar.addView(emptySegment)
        }
    }
}