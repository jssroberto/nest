package itson.appsmoviles.nest.ui.expenses


import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Category
import itson.appsmoviles.nest.ui.expenses.manager.CategoryManager
import itson.appsmoviles.nest.ui.expenses.manager.ExpenseProgressManager
import itson.appsmoviles.nest.ui.expenses.manager.CategorySelectionManager
import itson.appsmoviles.nest.ui.expenses.manager.FilterManager
import itson.appsmoviles.nest.ui.expenses.drawable.PieChartDrawable
import itson.appsmoviles.nest.ui.expenses.manager.ExpensesController
import itson.appsmoviles.nest.ui.util.formatDateShortForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker

class ExpensesFragment : Fragment() {

    private val categories = arrayListOf(
        Category(CategoryType.LIVING, 0.0f, R.color.category_living, 0.0f),
        Category(CategoryType.RECREATION, 0.0f, R.color.category_recreation, 0.0f),
        Category(CategoryType.TRANSPORT, 0.0f, R.color.category_transport, 0.0f),
        Category(CategoryType.FOOD, 0.0f, R.color.category_food, 0.0f),
        Category(CategoryType.HEALTH, 0.0f, R.color.category_health, 0.0f),
        Category(CategoryType.OTHER, 0.0f, R.color.category_other, 0.0f)
    )

    private lateinit var categorySelectionManager: CategorySelectionManager
    private lateinit var categoryManager: CategoryManager
    private lateinit var filterManager: FilterManager
    private lateinit var pieChartDrawable: PieChartDrawable
    private lateinit var expensesController: ExpensesController
    private lateinit var expenseProgressManager: ExpenseProgressManager

    private val viewModel: FilteredExpensesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FilteredExpensesViewModel() as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_expenses, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startDateButton = view.findViewById<Button>(R.id.btn_date_income)
        val endDateButton = view.findViewById<Button>(R.id.btn_end_date)
        val spinner = view.findViewById<Spinner>(R.id.spinner_categories_income)
        val filterButton = view.findViewById<Button>(R.id.btn_filter)
        val clearFiltersButton = view.findViewById<ImageButton>(R.id.btn_delete_filters)


        setUpSpinner(requireContext(), spinner)
        categoryManager = CategoryManager(categories)
        filterManager = FilterManager(requireContext(), startDateButton, endDateButton, spinner)
        filterManager.setup()
        expenseProgressManager = ExpenseProgressManager(requireContext())


        val (textViews, selectionManager) = setup(
            requireContext(), view, categories
        ) { selectedName ->
            expensesController.selectedCategoryName = selectedName
            pieChartDrawable.selectedCategory = categories.find { it.type.displayName == selectedName }
            view.findViewById<View>(R.id.graph).invalidate()
        }

        categorySelectionManager = selectionManager
        pieChartDrawable = configure(
            requireContext(),
            view.findViewById(R.id.graph),
            categories,
            textViews
        ) { selectedName ->
            expensesController.selectedCategoryName = selectedName
        }

        expensesController = ExpensesController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            rootView = view,
            viewModel = viewModel,
            categoryManager = categoryManager,
            filterManager = filterManager,
            pieChartDrawable = pieChartDrawable,
            progressManager = expenseProgressManager
        )


        filterButton.setOnClickListener {
            expensesController.filterAndLoadExpenses()
        }

        clearFiltersButton.setOnClickListener {
            filterManager.clearFilters()
            expensesController.filterAndLoadExpenses()
        }


        expensesController.loadExpenses()
    }
}
