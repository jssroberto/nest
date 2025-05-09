package itson.appsmoviles.nest.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import itson.appsmoviles.nest.ui.home.filter.FilterCriteria

    class SharedMovementsViewModel : ViewModel() {
        private val _movementDataChanged = MutableLiveData<Unit>()
        val movementDataChanged: LiveData<Unit> = _movementDataChanged

        private val _filterCriteria = MutableLiveData(FilterCriteria()) // Assuming FilterCriteria is defined
        val filterCriteria: LiveData<FilterCriteria> = _filterCriteria

        private val _userNameUpdated = MutableLiveData<Unit>()
        val userNameUpdated: LiveData<Unit> = _userNameUpdated

        private val _budgetDataChanged = MutableLiveData<Unit>()
        val budgetDataChanged: LiveData<Unit> = _budgetDataChanged

        fun notifyMovementDataChanged() {
            _movementDataChanged.value = Unit
        }

        fun updateFilters(newCriteria: FilterCriteria) {
            _filterCriteria.value = newCriteria
        }

        fun clearFilters() {
            _filterCriteria.value = FilterCriteria()
        }

        fun signalUserNameUpdated() {
            _userNameUpdated.value = Unit
        }

        fun notifyBudgetDataChanged() {
            _budgetDataChanged.value = Unit
        }
    }