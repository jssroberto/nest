package itson.appsmoviles.nest.ui.home.filter

import itson.appsmoviles.nest.data.enum.CategoryType

data class FilterCriteria(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val movementType: String? = null, // e.g., "Incomes", "Expenses", or null/"" for all
    val category: CategoryType? = null
) {
    // Helper to check if any filter is active besides default state
    fun hasActiveFilters(): Boolean {
        return startDate != null || endDate != null || !movementType.isNullOrEmpty() || category != null
    }
}