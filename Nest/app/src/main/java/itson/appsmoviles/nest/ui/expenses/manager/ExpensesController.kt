package itson.appsmoviles.nest.ui.expenses.manager

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.BudgetRepository
import itson.appsmoviles.nest.ui.budget.BudgetViewModel
import itson.appsmoviles.nest.ui.expenses.FilteredExpensesViewModel
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class ExpensesController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val rootView: View,
    private val viewModel: FilteredExpensesViewModel,
    private val categoryManager: CategoryManager,
    private val filterManager: FilterManager,
    private val pieChartDrawable: PieChartDrawable,
    private val progressManager: ExpenseProgressManager,
    private val budgetViewModel: BudgetViewModel // Añade esta dependencia
) {
    var selectedCategoryName: String? = null
    private val budgetRepository = BudgetRepository()

    init {
        setupBudgetObserver()
        setupExpensesObserver()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupBudgetObserver() {
        budgetViewModel.categoryBudgets.observe(lifecycleOwner) { budgets ->
            loadExpenses()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupExpensesObserver() {
        viewModel.getFilteredExpenses(
            filterManager.startTimestamp,
            filterManager.endTimestamp,
            selectedCategoryName
        ).observe(lifecycleOwner) { expenses ->
            updateUIWithExpenses(expenses)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUIWithExpenses(expenses: List<Expense>) {
        updateChartWithExpenses(expenses)
        updateProgressBars(expenses)
    }


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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateProgressBars(expenses: List<Expense>) {
        val expenseSums: Map<String, Float> = categoryManager.calculateCategorySums(expenses)

        lifecycleOwner.lifecycleScope.launch{
            val budgetData = withContext(Dispatchers.IO) {
                budgetRepository.getBudgetDataSuspend()
            }

            val expenseSumsByCategory: Map<CategoryType, Float> =
                expenseSums.mapKeys { (key, _) ->
                    CategoryType.fromName(key)
                }

            val targets: Map<CategoryType, Float> = CategoryType.values().associateWith { category ->
                budgetData
                    ?.categoryBudgets
                    ?.get(category.name)
                    ?.categoryBudget
                    ?.toFloat()
                    ?: 0f
            }

            progressManager.updateProgressBars(
                rootView,
                expenseSumsByCategory,
                targets
            )
        }

    }




}
