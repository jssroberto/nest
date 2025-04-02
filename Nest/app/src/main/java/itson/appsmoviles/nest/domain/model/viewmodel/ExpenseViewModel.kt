package itson.appsmoviles.nest.domain.model.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.domain.model.repository.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    private val _expenses = MutableLiveData<List<Expense>>()
    val expenses: LiveData<List<Expense>> get() = _expenses

    fun fetchExpenses() {
        viewModelScope.launch {
            _expenses.value = repository.getMovementsFromFirebase()
        }
    }

    fun addExpense(
        amount: Double,
        description: String,
        category: Category,
        paymentMethod: String,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            repository.addExpense(amount, description, category, paymentMethod, date, onSuccess, onFailure)
        }
    }
}
