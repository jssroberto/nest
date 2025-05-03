package itson.appsmoviles.nest.ui.add.income

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.data.repository.IncomeRepository
import kotlinx.coroutines.launch

class AddIncomeViewModel : ViewModel() {
    private val repository = IncomeRepository()

    private val _isIncomeAdded = MutableLiveData<Boolean>()
    val isIncomeAdded: LiveData<Boolean> get() = _isIncomeAdded

    fun addIncome(
        income: Income,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            repository.addIncome(
                income,
                onSuccess,
                onFailure
            )
        }
    }
}
