package itson.appsmoviles.nest.presentation.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.Movement
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.presentation.adapter.MovementAdapter
import java.time.LocalDateTime
import kotlin.collections.iterator

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

        val progressContainer = view.findViewById<LinearLayout>(R.id.progress_bar)

        val totalBudget = 100


        val expenses = mapOf(
            "Food" to 10,
            "Gas" to 20,
            "Entertainment" to 15
        )

        val categoryColors = mapOf(
            "Food" to "#FFA500", // Orange
            "Gas" to "#00BFFF",  // Blue
            "Entertainment" to "#32CD32" // Green
        )

        // Dynamically create segments
        for ((category, amount) in expenses) {
            val barSegment = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor(categoryColors[category]))
                layoutParams = LinearLayout.LayoutParams(
                    0,  // Width is weighted dynamically
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    amount.toFloat() / totalBudget // Weight proportionally
                )
            }
            progressContainer.addView(barSegment)
        }



        val recyclerView = view.findViewById<RecyclerView>(R.id.home_recycler_view)

        val movements = listOf(
            Movement(1, Category.FOOD, "Groceries", 10.0f, LocalDateTime.now()),
            Movement(2, Category.TRANSPORT, "Gasoline", 20.0f, LocalDateTime.now()),
            Movement(3, Category.RECREATION, "Movie", 15.0f, LocalDateTime.now())
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = MovementAdapter(movements)
    }
}