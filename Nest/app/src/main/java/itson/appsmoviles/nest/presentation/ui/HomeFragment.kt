package itson.appsmoviles.nest.presentation.ui

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.domain.model.viewmodel.ExpenseViewModel
import itson.appsmoviles.nest.presentation.adapter.MovementAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime



class HomeFragment : Fragment() {
    private lateinit var progressContainer: LinearLayout
    private val totalBudget = 100.0f
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAdd: ImageButton
    private lateinit var bottonNav: BottomNavigationView
    private lateinit var btnFilter: ImageButton
    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔔 Escucha si un gasto fue actualizado desde otro fragmento
        parentFragmentManager.setFragmentResultListener(
            "update_expense_result",
            viewLifecycleOwner
        ) { _, result ->
            val wasUpdated = result.getBoolean("updated", false)
            if (wasUpdated) {
                Log.d("HOME_FRAGMENT", "Se actualizó un gasto. Recargando lista.")
                viewModel.fetchExpenses()
            }
        }

        // Resto de tu código...
        progressContainer = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.home_recycler_view)
        btnAdd = view.findViewById(R.id.btn_add)
        bottonNav = requireActivity().findViewById(R.id.bottomNavigation)
        btnFilter = view.findViewById(R.id.btn_filter_home)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            val sortedExpenses = expenses.sortedByDescending { it.date }
            initRecyclerView(sortedExpenses)
            val expenseMap = calculateExpenses(sortedExpenses)
            paintBudget(expenseMap)
        }

        viewModel.fetchExpenses()

        btnAdd.setOnClickListener {
            changeAddFragment()
        }

        applyBtnAddMargin()

        btnFilter.setOnClickListener {
            val dialog = FilterMovementsFragment()
            dialog.show(parentFragmentManager, "FilterMovementsFragment")
        }
    }


    private fun calculateExpenses(expenses: List<Expense>): Map<Category, Float> {
        return expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
    }



    private fun initRecyclerView(expenses: List<Expense>) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = false
        }
        recyclerView.adapter = MovementAdapter(expenses)

        val dividerItemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        val divider = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        divider?.let { dividerItemDecoration.setDrawable(it) }
        recyclerView.addItemDecoration(dividerItemDecoration)
    }



    private fun paintBudget(expenses: Map<Category, Float>) {
        progressContainer.removeAllViews()

        val categoryColors = getCategoryColors()
        for ((category, amount) in expenses) {
            val barSegment = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor(categoryColors[category]))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    amount / totalBudget
                )
            }
            progressContainer.addView(barSegment)
        }

        val usedBudget = expenses.values.sum()
        paintRemainingBudget(usedBudget)
    }


    private fun paintRemainingBudget(usedBudget: Float) {
        val remainingBudget = totalBudget - usedBudget
        if (remainingBudget > 0) {
            val emptySegment = View(requireContext()).apply {
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    remainingBudget / totalBudget
                )
            }
            progressContainer.addView(emptySegment)
        }
    }


    private fun getCategoryColors(): Map<Category, String> {
        fun colorToHex(colorResId: Int): String {
            val colorInt = ContextCompat.getColor(requireContext(), colorResId)
            return String.format("#%06X", 0xFFFFFF and colorInt)
        }

        return mapOf(
            Category.LIVING to colorToHex(R.color.category_living),
            Category.RECREATION to colorToHex(R.color.category_recreation),
            Category.TRANSPORT to colorToHex(R.color.category_transport),
            Category.FOOD to colorToHex(R.color.category_food),
            Category.HEALTH to colorToHex(R.color.category_health),
            Category.OTHER to colorToHex(R.color.category_other)
        )
    }


    private fun getCategoryFromString(categoryName: String): Category {
        return when (categoryName.lowercase()) {
            "food" -> Category.FOOD
            "transport" -> Category.TRANSPORT
            "entertainment" -> Category.RECREATION
            "home" -> Category.LIVING
            "health" -> Category.HEALTH
            "other" -> Category.OTHER
            else -> Category.OTHER  // Categoría por defecto
        }
    }


    private fun changeAddFragment() {
        val newFragment = AddFragment()
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, newFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }


    private fun applyBtnAddMargin() {
        bottonNav.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottonNav.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val bottonNavHeight = bottonNav.height
                val params = btnAdd.layoutParams as FrameLayout.LayoutParams
                params.bottomMargin = bottonNavHeight + 10.dp
                btnAdd.layoutParams = params
            }
        })
    }

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}
