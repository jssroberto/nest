package itson.appsmoviles.nest.ui.add.expense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository
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
        categoryType: CategoryType,
        paymentMethod: String,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            repository.addExpense(amount, description, categoryType, paymentMethod, date, onSuccess, onFailure)
        }
    }
}