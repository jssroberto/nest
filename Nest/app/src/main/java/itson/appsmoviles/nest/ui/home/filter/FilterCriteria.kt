package itson.appsmoviles.nest.ui.home.filter

import itson.appsmoviles.nest.data.enums.CategoryType
import java.time.LocalDate

data class FilterCriteria(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val categoryType: CategoryType? = null
) {
    fun isActive(): Boolean {
        return startDate != null || endDate != null || categoryType != null
    }

    companion object {
        const val KEY_START_DATE = "startDateEpochDay"
        const val KEY_END_DATE = "endDateEpochDay"
        const val KEY_CATEGORY_NAME = "categoryName"
        const val KEY_CLEAR_FILTERS = "clearFilters"
        const val RESULT_KEY = "filter_result"
    }
}