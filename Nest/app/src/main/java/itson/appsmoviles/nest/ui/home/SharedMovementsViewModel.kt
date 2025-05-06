package itson.appsmoviles.nest.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import itson.appsmoviles.nest.ui.home.filter.FilterCriteria

class SharedMovementsViewModel : ViewModel() {
    private val _movementDataChanged = MutableLiveData<Unit>()
    val movementDataChanged: LiveData<Unit> = _movementDataChanged

    private val _filterCriteria = MutableLiveData(FilterCriteria()) // Initialize with default (no filters)
    val filterCriteria: LiveData<FilterCriteria> = _filterCriteria

    fun notifyMovementDataChanged() {
        _movementDataChanged.value = Unit
    }

    fun updateFilters(newCriteria: FilterCriteria) {
        _filterCriteria.value = newCriteria
    }

    fun clearFilters() {
        _filterCriteria.value = FilterCriteria()
    }
}