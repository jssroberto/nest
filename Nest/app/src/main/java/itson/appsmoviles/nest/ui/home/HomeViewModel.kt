package itson.appsmoviles.nest.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.data.model.Movement
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.data.repository.MovementRepository
import itson.appsmoviles.nest.ui.common.UiState
import itson.appsmoviles.nest.ui.home.filter.FilterCriteria
import itson.appsmoviles.nest.ui.home.state.HomeOverviewState
import itson.appsmoviles.nest.ui.home.state.MovementsState
import itson.appsmoviles.nest.ui.util.unaccent
import kotlinx.coroutines.launch
import java.util.Locale

class HomeViewModel(
    private val sharedMovementsViewModel: SharedMovementsViewModel
) : ViewModel() {

    private val expenseRepository: ExpenseRepository = ExpenseRepository()
    private val movementRepository: MovementRepository = MovementRepository()

    private val _overviewState = MutableLiveData<UiState<HomeOverviewState>>(UiState.Loading)
    val overviewState: LiveData<UiState<HomeOverviewState>> get() = _overviewState

    private val _movementsState = MediatorLiveData<UiState<MovementsState>>(UiState.Loading)
    val movementsState: LiveData<UiState<MovementsState>> get() = _movementsState

    private var fullMovementsList: List<Movement> = listOf()
    private var fullExpensesList: List<Expense> = listOf()
    private var currentSearchQuery: String = ""
    private var currentFilterCriteria: FilterCriteria = FilterCriteria()

    private val _fetchedMovements = MutableLiveData<UiState<List<Movement>>>(UiState.Loading)


    init {
        _movementsState.addSource(sharedMovementsViewModel.filterCriteria) { criteria ->
            currentFilterCriteria = criteria
            applyFilterAndSearch()
        }

        _movementsState.addSource(_fetchedMovements) { fetchedState ->
            when (fetchedState) {
                is UiState.Loading -> _movementsState.value = UiState.Loading
                is UiState.Success -> {
                    fullMovementsList = fetchedState.data
                    fullExpensesList = fullMovementsList.filterIsInstance<Expense>()
                    applyFilterAndSearch()
                }

                is UiState.Error -> _movementsState.value = UiState.Error(fetchedState.message)
            }
        }

        refreshAllData()
    }

    fun refreshAllData() {
        fetchOverviewData()
        fetchMovementsInternal()
    }

    fun fetchOverviewData() {
        viewModelScope.launch {
            _overviewState.value = UiState.Loading
            val overviewData = movementRepository.getOverviewData()

            if (overviewData != null) {
                _overviewState.value = UiState.Success(overviewData)
            } else {
                _overviewState.value = UiState.Error("Failed to load overview data.")
            }
        }
    }

    private fun fetchMovementsInternal() {
        viewModelScope.launch {
            _movementsState.value = UiState.Loading
            try {
                fullMovementsList =
                    movementRepository.getAllMovements().sortedByDescending { it.date }
                fullExpensesList = expenseRepository.getAllExpenses().sortedByDescending { it.date }
                applyFilterAndSearch()
            } catch (e: Exception) {
                _movementsState.value = UiState.Error("Failed to load expenses: ${e.message}")
            }
        }
    }

    fun applySearchQuery(query: String) {
        currentSearchQuery = query.lowercase(Locale.getDefault()).trim().unaccent()
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        if (fullMovementsList.isEmpty() && _fetchedMovements.value !is UiState.Success) {
            return
        }

        var filteredList = fullMovementsList

        currentFilterCriteria.startDate?.let { start ->
            filteredList = filteredList.filter { it.date >= start }
        }
        currentFilterCriteria.endDate?.let { end ->
            filteredList = filteredList.filter { it.date <= end }
        }

        when (currentFilterCriteria.movementType) {
            "Incomes" -> filteredList = filteredList.filterIsInstance<Income>()
            "Expenses" -> filteredList = filteredList.filterIsInstance<Expense>()
        }

        currentFilterCriteria.category?.let { category ->
            filteredList = filteredList.filter { movement ->
                movement is Expense && movement.category == category
            }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { movement ->
                val description = movement.description.lowercase(Locale.getDefault()).unaccent()
                val amount = movement.amount.toString()
                val categoryMatch = if (movement is Expense) {
                    movement.category.name.lowercase(Locale.getDefault()).unaccent()
                        .contains(currentSearchQuery)
                } else {
                    false
                }
                description.contains(currentSearchQuery) || amount.contains(currentSearchQuery) || categoryMatch
            }
        }

        val categoryTotals = calculateCategoryTotals(fullExpensesList)

        _movementsState.value = UiState.Success(
            MovementsState(
                displayedExpenses = filteredList,
                categoryTotals = categoryTotals
            )
        )
    }

    private fun calculateCategoryTotals(expenses: List<Expense>): Map<CategoryType, Double> {
        return expenses
            .groupBy { it.category }
            .mapValues { (_, expensesInCategory) ->
                expensesInCategory.sumOf { it.amount }
            }
    }
}