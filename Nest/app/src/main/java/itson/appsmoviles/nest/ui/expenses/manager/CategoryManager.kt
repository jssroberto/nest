package itson.appsmoviles.nest.ui.expenses.manager

import itson.appsmoviles.nest.data.model.Category
import itson.appsmoviles.nest.data.model.Expense

class CategoryManager(private val categories: MutableList<Category>) {

    fun calculateTotal(): Float {
        return categories.sumOf { it.total.toDouble() }.toFloat()
    }

    fun updateWithExpenses(expenses: List<Expense>) {
        categories.forEach { it.total = 0.0f }

        val grouped = expenses.groupBy { it.category.displayName }
        grouped.forEach { (name, list) ->
            val total = list.sumOf { it.amount.toDouble() }.toFloat()
            categories.find { it.type.displayName == name }?.total = total
        }
    }

    fun calculateCategorySums(expenses: List<Expense>): Map<String, Float> {
        return expenses.groupBy { it.category.displayName }
            .mapValues { entry ->
                entry.value.sumOf { it.amount.toDouble() }.toFloat()
            }
    }

}
