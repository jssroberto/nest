package itson.appsmoviles.nest.presentation.ui

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.Movement
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.presentation.adapter.MovementAdapter
import java.time.LocalDateTime

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var progressContainer: LinearLayout
    private val totalBudget = 100.0f
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAdd: ImageButton
    private lateinit var bottonNav: BottomNavigationView
    private lateinit var btnFilter: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)



        return view

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressContainer = view.findViewById<LinearLayout>(R.id.progress_bar)
        recyclerView = view.findViewById<RecyclerView>(R.id.home_recycler_view)
        btnAdd = view.findViewById<ImageButton>(R.id.btn_add)
        bottonNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)
        btnFilter = view.findViewById<ImageButton>(R.id.btn_filter_home)

        val expenses = getExpenses()

        paintBudget(expenses)

        val movements = getMovements()

        initRecyclerView(movements)

        btnAdd.setOnClickListener {
            changeAddFragment()
        }

        applyBtnAddMargin()

        btnFilter.setOnClickListener {
            val dialog = FilterMovementsFragment()
            dialog.show(parentFragmentManager, "FilterMovementsFragment")
        }


    }

    private fun initRecyclerView(movements: List<Movement>) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = MovementAdapter(movements)
        val dividerItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        val divider = ContextCompat.getDrawable(requireContext(), R.drawable.divider)

        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }

        recyclerView.addItemDecoration(dividerItemDecoration)
    }

    private fun paintBudget(expenses: Map<Category, Float>) {
        val categoryColors = getCategoryColors()
        for ((category, amount) in expenses) {
            val barSegment = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor(categoryColors[category]))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    amount.toFloat() / totalBudget
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
                    remainingBudget.toFloat() / totalBudget
                )
            }
            progressContainer.addView(emptySegment)
        }
    }

    private fun getExpenses(): Map<Category, Float> {
        //TODO replace this with actual data
        return mapOf(
            Category.LIVING to 10.0f,
            Category.RECREATION to 20.0f,
            Category.TRANSPORT to 15.0f,
            Category.FOOD to 5.0f,
            Category.HEALTH to 10.0f,
            Category.OTHER to 10.0f

        )
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getMovements(): List<Movement> {
        //TODO replace this with actual data
        return listOf(
            Movement(1, Category.FOOD, "Groceries", 50.0f, LocalDateTime.now().minusDays(1)),
            Movement(2, Category.TRANSPORT, "Bus Ticket", 2.5f, LocalDateTime.now().minusDays(2)),
            Movement(3, Category.RECREATION, "Cinema", 12.0f, LocalDateTime.now().minusDays(3)),
            Movement(4, Category.LIVING, "Rent", 500.0f, LocalDateTime.now().minusDays(4)),
            Movement(5, Category.HEALTH, "Medicine", 30.0f, LocalDateTime.now().minusDays(5)),
            Movement(6, Category.OTHER, "Gift", 25.0f, LocalDateTime.now().minusDays(6)),
            Movement(7, Category.FOOD, "Restaurant", 60.0f, LocalDateTime.now().minusDays(7)),
            Movement(8, Category.TRANSPORT, "Taxi", 15.0f, LocalDateTime.now().minusDays(8)),
            Movement(9, Category.RECREATION, "Concert", 100.0f, LocalDateTime.now().minusDays(9)),
            Movement(10, Category.LIVING, "Utilities", 80.0f, LocalDateTime.now().minusDays(10))
        )
    }

    private fun changeAddFragment(){
        val newFragment = AddFragment()

        // Start a fragment transaction
        val transaction = parentFragmentManager.beginTransaction()

        // Optionally, add this transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment)  // Replace the container's content with the new fragment
        transaction.addToBackStack(null)  // Optional, adds this transaction to the back stack

        // Commit the transaction to apply the change
        transaction.commit()
    }

    private fun applyBtnAddMargin(){
        bottonNav.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
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