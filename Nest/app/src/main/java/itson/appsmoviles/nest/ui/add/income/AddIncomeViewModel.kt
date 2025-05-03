package itson.appsmoviles.nest.ui.add.income

import android.R.attr.category
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.repository.IncomeRepository
import kotlinx.coroutines.launch

class AddIncomeViewModel : ViewModel() {
    private val repository = IncomeRepository()

    private val _isIncomeAdded = MutableLiveData<Boolean>()
    val isIncomeAdded: LiveData<Boolean> get() = _isIncomeAdded

    fun addIncome(amount: Double, date: Long, description: String) {
        viewModelScope.launch {
            repository.addIncome(
                amount,
                date,
                description,
                onSuccess = { _isIncomeAdded.value = true },
                onFailure = {
                    Log.e("IncomeViewModel", "Error adding income", it)
                    _isIncomeAdded.value = false
                }
            )
        }
    }
}
