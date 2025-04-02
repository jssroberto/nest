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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.Movement
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.presentation.adapter.MovementAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

        progressContainer = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.home_recycler_view)
        btnAdd = view.findViewById(R.id.btn_add)
        bottonNav = requireActivity().findViewById(R.id.bottomNavigation)
        btnFilter = view.findViewById(R.id.btn_filter_home)

        lifecycleScope.launch {
            val movements = getMovementsFromFirebase()
            Log.d("HomeFragment", "Movements retrieved: $movements")  // Agrega esto para depuración
            initRecyclerView(movements)

            val expenses = calculateExpenses(movements)
            paintBudget(expenses)
        }


        btnAdd.setOnClickListener {
            changeAddFragment()
        }

        applyBtnAddMargin()

        btnFilter.setOnClickListener {
            val dialog = FilterMovementsFragment()
            dialog.show(parentFragmentManager, "FilterMovementsFragment")
        }
    }

    private fun calculateExpenses(movements: List<Movement>): Map<Category, Float> {
        return movements.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getMovementsFromFirebase(): List<Movement> {
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot: DataSnapshot = database.child("usuarios").child(userId).child("gastos").get().await()
            Log.d("HomeFragment", "Firebase snapshot children count: ${snapshot.childrenCount}")

            snapshot.children.mapNotNull { gastoSnapshot ->
                val gasto = gastoSnapshot.getValue(Expense::class.java)
                Log.d("HomeFragment", "Gasto leído: $gasto")

                gasto?.let {
                    Movement(
                        category = getCategoryFromString(gasto.categoria),
                        description = gasto.descripcion,
                        amount = gasto.monto.toFloat(),
                        payment = gasto.payment,
                        date = LocalDateTime.now().minusDays(1)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error obteniendo datos de Firebase", e)
            emptyList()
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



    fun getCategoryFromString(categoryName: String): Category {
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

    private fun changeAddFragment(){
        val newFragment = AddFragment()


        val transaction = parentFragmentManager.beginTransaction()


        transaction.replace(R.id.fragment_container, newFragment)
        transaction.addToBackStack(null)


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