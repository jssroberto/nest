package itson.appsmoviles.nest.ui.add.expense

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFilteredExpenses(
        startDate: Long?,
        endDate: Long?,
        selectedCategoryDisplayName: String?
    ): LiveData<List<Expense>> = liveData {
        val cleanedCategory = selectedCategoryDisplayName?.takeIf {
            it.isNotBlank() && it != "Select a category"
        }

        val category = cleanedCategory?.let { CategoryType.fromDisplayName(it) }

        val expenses = expenseRepository.getFilteredExpensesFromFirebase(
            startDate,
            endDate,
            category?.displayName // Pasa null si no hay categoría válida
        )

        emit(expenses)
    }

}
