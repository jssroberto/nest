package itson.appsmoviles.nest.ui.home

import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Movement

data class MovementsState(
    val displayedExpenses: List<Movement> = emptyList(),
    val categoryTotals: Map<CategoryType, Double> = emptyMap()
)