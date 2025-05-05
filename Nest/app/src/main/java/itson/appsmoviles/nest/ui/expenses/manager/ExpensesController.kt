package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.expenses.FilteredExpensesViewModel
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable

class ExpensesController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val rootView: View,
    private val viewModel: FilteredExpensesViewModel,
    private val categoryManager: CategoryManager,
    private val filterManager: FilterManager,
    private val pieChartDrawable: PieChartDrawable,
    private val progressManager: ExpenseProgressManager // 👈 NUEVO parámetro
) {
    var selectedCategoryName: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun filterAndLoadExpenses() {
        selectedCategoryName = filterManager.getSelectedCategory()
        viewModel.getFilteredExpenses(
            filterManager.startTimestamp,
            filterManager.endTimestamp,
            selectedCategoryName
        ).observe(lifecycleOwner) { expenses ->
            updateChartWithExpenses(expenses)
            updateProgressBars(expenses)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadExpenses() {
        viewModel.getFilteredExpenses(
            filterManager.startTimestamp,
            filterManager.endTimestamp,
            selectedCategoryName
        ).observe(lifecycleOwner) { expenses ->
            updateChartWithExpenses(expenses)
            updateProgressBars(expenses)
        }
    }

    private fun updateChartWithExpenses(expenses: List<Expense>) {
        categoryManager.updateWithExpenses(expenses)
        pieChartDrawable.invalidateSelf()
        updateTotal()
    }

    private fun updateTotal() {
        rootView.findViewById<TextView>(R.id.totalExpenses).text = "$${categoryManager.calculateTotal()}"
    }

    private fun updateProgressBars(expenses: List<Expense>) {
        val expenseSums = categoryManager.calculateCategorySums(expenses)

        val targets = mapOf(
            "Food" to 75f,
            "Transport" to 1f,
            "Health" to 50f,
            "Others" to 50f,
            "Home" to 41f,
            "Recreation" to 20f
        )

        progressManager.updateProgressBars(rootView, expenseSums, targets)
    }
}
