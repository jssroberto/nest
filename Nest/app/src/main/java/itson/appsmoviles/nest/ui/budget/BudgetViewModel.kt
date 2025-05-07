package itson.appsmoviles.nest.ui.budget

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {

    private val repository = BudgetRepository()
    val totalBudget = MutableLiveData<Float>()
    val categoryBudgets = MutableLiveData<Map<CategoryType, Float>>()

    private var lastTotalBudget: Float? = null
    private var lastCategoryBudgets: Map<CategoryType, Float>? = null

    init {
        loadBudgetData()
    }

    private fun loadBudgetData() {
        repository.getBudgetData { budget ->
            totalBudget.value = budget.totalBudget
            categoryBudgets.value = budget.categoryBudgets?.mapKeys { entry ->
                try {
                    CategoryType.valueOf(entry.key)
                } catch (e: IllegalArgumentException) {
                    CategoryType.OTHER
                }
            }?.toMap() ?: emptyMap()
        }
    }

    fun setTotalBudget(amount: Float) {
        // Solo actualizar si el valor ha cambiado
        if (totalBudget.value != amount) {
            totalBudget.value = amount
            saveBudgetToRepository()
        }
    }

    fun setCategoryBudget(categoryName: String, amount: Float) {
        val categoryType = CategoryType.valueOf(categoryName)
        val updatedCategoryBudgets = categoryBudgets.value?.toMutableMap() ?: mutableMapOf()
        updatedCategoryBudgets[categoryType] = amount
        categoryBudgets.value = updatedCategoryBudgets

        // Solo guardar si hubo un cambio en el presupuesto de categorías
        if (categoryBudgets.value != lastCategoryBudgets) {
            saveBudgetToRepository()
        }
    }

    private fun saveBudgetToRepository() {
        val budgetToSave = Budget(
            totalBudget = totalBudget.value ?: 0f,
            categoryBudgets = categoryBudgets.value?.mapKeys { it.key.name } ?: emptyMap()
        )

        // Guardar en Firebase solo si hay cambios
        repository.saveBudget(budgetToSave) {
            Log.d("BudgetViewModel", "Budget data saved successfully.")
        }

        // Actualizar los valores para evitar escrituras repetidas
        lastTotalBudget = totalBudget.value
        lastCategoryBudgets = categoryBudgets.value
    }
}
