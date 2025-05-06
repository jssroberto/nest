package itson.appsmoviles.nest.ui.home

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.common.UiState
import itson.appsmoviles.nest.ui.home.adapter.MovementAdapter
import itson.appsmoviles.nest.ui.home.drawable.ExpensesBarPainter
import itson.appsmoviles.nest.ui.home.filter.FilterMovementsFragment
import itson.appsmoviles.nest.ui.home.state.MovementsState
import itson.appsmoviles.nest.ui.main.MainActivity
import itson.appsmoviles.nest.ui.util.showToast

@RequiresApi(Build.VERSION_CODES.O)
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by activityViewModels {
        Log.d("VM_FACTORY_CALL", "HomeVM Factory CALLED for HomeFragment. Current HomeFrag.SharedVM hash for injection: ${System.identityHashCode(sharedMovementsViewModel)}")
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                Log.d("VM_FACTORY_CREATE", "HomeVM Factory create(): Injecting SharedVM: ${System.identityHashCode(sharedMovementsViewModel)} into new HomeViewModel for HomeFragment")
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(sharedMovementsViewModel) as T // Pass the correct instance
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()

    private lateinit var movementAdapter: MovementAdapter
    private lateinit var expensesBarPainter: ExpensesBarPainter

    private lateinit var txtWelcome: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpenses: TextView
    private lateinit var txtNetBalance: TextView
    private lateinit var txtBudget: TextView
    private lateinit var expensesBar: LinearLayout
    private lateinit var edtSearchHome: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAdd: ImageButton
    private lateinit var bottonNav: BottomNavigationView

    private var lastExpensesData: MovementsState? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerView()
        setUpClickListeners()
        setupSearchListener()
        applyBtnAddMargin()
        observeViewModels()
    }

    private fun bindViews(view: View) {
        txtWelcome = view.findViewById(R.id.txt_welcome_home)
        txtIncome = view.findViewById(R.id.txt_income_home)
        txtExpenses = view.findViewById(R.id.txt_expenses_home)
        txtNetBalance = view.findViewById(R.id.txt_net_balance_home)
        txtBudget = view.findViewById(R.id.txt_budget_home)
        expensesBar = view.findViewById(R.id.expenses_bar)
        edtSearchHome = view.findViewById(R.id.edt_search_home)
        btnFilter = view.findViewById(R.id.btn_filter_home)
        recyclerView = view.findViewById(R.id.home_recycler_view)
        btnAdd = view.findViewById(R.id.btn_add)
        bottonNav = requireActivity().findViewById(R.id.bottomNavigation)
    }

    private fun setupRecyclerView() {
        movementAdapter = MovementAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = movementAdapter

        val dividerItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

    private fun setUpClickListeners() {
        btnAdd.setOnClickListener {
            (activity as? MainActivity)?.showAddFragment()
        }
        btnFilter.setOnClickListener {
            val dialog = FilterMovementsFragment()
            dialog.show(parentFragmentManager, "FilterMovementsFragment")
        }
    }

    private fun setupSearchListener() {
        edtSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.applySearchQuery(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModels() {
        viewModel.overviewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                }

                is UiState.Success -> {
                    val overview = state.data
                    txtWelcome.text = "Hi ${overview.userName}\nhere's your monthly overview"
                    txtIncome.text = "$${overview.totalIncome.toInt()}"
                    txtExpenses.text = "$${overview.totalExpenses.toInt()}"
                    val balance = overview.netBalance.toInt()
                    if (balance < 0) {
                        txtNetBalance.text = "-$${kotlin.math.abs(balance)}"
                    } else {
                        txtNetBalance.text = "$$${balance}"
                    }
                    txtBudget.text = "$${overview.budget.toInt()}"

                    val painterNeedsInitialization = !::expensesBarPainter.isInitialized

                    if (painterNeedsInitialization) {
                        expensesBarPainter =
                            ExpensesBarPainter(requireContext(), expensesBar, overview.budget)
                        lastExpensesData?.let { cachedData ->
                            expensesBarPainter.paintBudget(cachedData.categoryTotals)
                        }

                    } else {
                        expensesBarPainter.updateBudget(overview.budget)
                    }
                }

                is UiState.Error -> {
                    Log.e("HomeFragment", "Error loading overview: ${state.message}")
                    showToast(requireContext(), "Error loading overview: ${state.message}")
                }
            }
        }

        viewModel.movementsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {

                }

                is UiState.Success -> {
                    val movementsData = state.data
                    movementAdapter.updateData(movementsData.displayedExpenses)
                    if (::expensesBarPainter.isInitialized) {
                        expensesBarPainter.paintBudget(movementsData.categoryTotals)
                    } else {
                        viewModel.overviewState.value?.let { overviewState ->
                            if (overviewState is UiState.Success) {
                                expensesBarPainter = ExpensesBarPainter(requireContext(), expensesBar, overviewState.data.budget)
                                expensesBarPainter.paintBudget(movementsData.categoryTotals)
                            }
                        }
                    }
                    lastExpensesData = movementsData
                }

                is UiState.Error -> {
                    lastExpensesData = null
                    Log.e("HomeFragment", "Error loading expenses: ${state.message}")
                    showToast(requireContext(), "Error loading expenses: ${state.message}")
                }
            }
        }

        sharedMovementsViewModel.movementDataChanged.observe(viewLifecycleOwner) {
            viewModel.refreshAllData()
        }
    }

    private fun applyBtnAddMargin() {
        bottonNav.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottonNav.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val bottonNavHeight = bottonNav.height
                (btnAdd.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
                    val marginDp = 10
                    val marginPx = (marginDp * resources.displayMetrics.density).toInt()
                    params.bottomMargin = bottonNavHeight + marginPx
                    btnAdd.layoutParams = params
                }
            }
        })
    }
}
