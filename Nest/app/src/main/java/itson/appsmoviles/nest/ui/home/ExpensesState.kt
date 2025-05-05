package itson.appsmoviles.nest.ui.home

import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense

data class ExpensesState(
    val displayedExpenses: List<Expense> = emptyList(),
    val categoryTotals: Map<CategoryType, Double> = emptyMap()
)