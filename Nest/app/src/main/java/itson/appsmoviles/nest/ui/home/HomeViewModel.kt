package itson.appsmoviles.nest.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.model.Movement
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.data.repository.MovementRepository
import itson.appsmoviles.nest.ui.common.UiState
import itson.appsmoviles.nest.ui.home.state.HomeOverviewState
import itson.appsmoviles.nest.ui.home.state.MovementsState
import itson.appsmoviles.nest.ui.util.unaccent
import kotlinx.coroutines.launch
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val expenseRepository: ExpenseRepository = ExpenseRepository()
    private val movementRepository: MovementRepository = MovementRepository()

    private val _overviewState = MutableLiveData<UiState<HomeOverviewState>>(UiState.Loading)
    val overviewState: LiveData<UiState<HomeOverviewState>> get() = _overviewState

    private val _movementsState = MutableLiveData<UiState<MovementsState>>(UiState.Loading)
    val movementsState: LiveData<UiState<MovementsState>> get() = _movementsState

    private var fullMovementsList: List<Movement> = listOf()
    private var fullExpensesList: List<Expense> = listOf()
    private var currentSearchQuery: String = ""

    init {
        refreshAllData()
    }

    fun refreshAllData() {
        fetchOverviewData()
        fetchMovementsInternal()
    }

    private fun fetchOverviewData() {
        viewModelScope.launch {
            _overviewState.value = UiState.Loading
            val overviewData = movementRepository.getOverviewData() // Returns HomeOverviewState?

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
                fullMovementsList = movementRepository.getAllMovements().sortedByDescending { it.date }
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
        val filteredList = if (currentSearchQuery.isEmpty()) {
            fullMovementsList
        } else {
            fullMovementsList.filter { expense ->
                val description = expense.description.lowercase(Locale.getDefault()).unaccent()
                //val category = expense.category.name.lowercase(Locale.getDefault()).unaccent()
                val amount = expense.amount.toString()

                description.contains(currentSearchQuery) ||
                        //category.contains(currentSearchQuery) ||
                        amount.contains(currentSearchQuery)
            }
        }

        // val categoryTotals = calculateCategoryTotals(filteredList) // Based on filtered
        val categoryTotals = calculateCategoryTotals(fullExpensesList) // Based on full list

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